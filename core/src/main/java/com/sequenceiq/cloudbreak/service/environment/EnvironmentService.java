package com.sequenceiq.cloudbreak.service.environment;

import static com.sequenceiq.cloudbreak.authorization.WorkspaceResource.ENVIRONMENT;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentAttachV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentChangeCredentialV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentDetachV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentEditV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.LocationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.RegisterDatalakeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.DetailedEnvironmentV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.SimpleEnvironmentV4Response;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.Coordinate;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.controller.validation.environment.ClusterCreationEnvironmentValidator;
import com.sequenceiq.cloudbreak.controller.validation.environment.EnvironmentAttachValidator;
import com.sequenceiq.cloudbreak.controller.validation.environment.EnvironmentCreationValidator;
import com.sequenceiq.cloudbreak.controller.validation.environment.EnvironmentDetachValidator;
import com.sequenceiq.cloudbreak.controller.validation.environment.EnvironmentRegionValidator;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.KubernetesConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.PlatformResourceRequest;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.domain.environment.Region;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.repository.environment.EnvironmentRepository;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.KubernetesConfigService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariClientProvider;
import com.sequenceiq.cloudbreak.service.credential.CredentialPrerequisiteService;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.kerberos.KerberosService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.platform.PlatformParameterService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.sharedservice.AmbariDatalakeConfigProvider;
import com.sequenceiq.cloudbreak.service.sharedservice.ServiceDescriptorDefinitionProvider;
import com.sequenceiq.cloudbreak.service.stack.StackApiViewService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class EnvironmentService extends AbstractWorkspaceAwareResourceService<Environment> {

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private KubernetesConfigService kubernetesConfigService;

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private ProxyConfigService proxyConfigService;

    @Inject
    private KerberosService kerberosService;

    @Inject
    private EnvironmentCredentialOperationService environmentCredentialOperationService;

    @Inject
    private EnvironmentCreationValidator environmentCreationValidator;

    @Inject
    private EnvironmentAttachValidator environmentAttachValidator;

    @Inject
    private EnvironmentRegionValidator environmentRegionValidator;

    @Inject
    private EnvironmentViewService environmentViewService;

    @Inject
    private StackService stackService;

    @Inject
    private StackApiViewService stackApiViewService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private EnvironmentRepository environmentRepository;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private PlatformParameterService platformParameterService;

    @Inject
    private EnvironmentDetachValidator environmentDetachValidator;

    @Inject
    private AmbariDatalakeConfigProvider ambariDatalakeConfigProvider;

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private ClusterCreationEnvironmentValidator clusterCreationEnvironmentValidator;

    @Inject
    private DatalakeResourcesService datalakeResourcesService;

    public Set<SimpleEnvironmentV4Response> listByWorkspaceId(Long workspaceId) {
        Set<SimpleEnvironmentV4Response> environmentResponses = environmentViewService.findAllByWorkspaceId(workspaceId).stream()
                .map(env -> conversionService.convert(env, SimpleEnvironmentV4Response.class))
                .collect(Collectors.toSet());
        for (SimpleEnvironmentV4Response environmentResponse : environmentResponses) {
            Set<String> datalakeNames = stackService.findDatalakeStackNamesByWorkspaceAndEnvironment(workspaceId, environmentResponse.getId());
            environmentResponse.setDatalakeClusterNames(datalakeNames);
            Set<String> workloadNames = stackService.findWorkloadStackNamesByWorkspaceAndEnvironment(workspaceId, environmentResponse.getId());
            environmentResponse.setWorkloadClusterNames(workloadNames);
            Set<String> datalakeResourcesNames =
                    datalakeResourcesService.findDatalakeResourcesNamesByWorkspaceAndEnvironment(workspaceId, environmentResponse.getId());
            environmentResponse.setDatalakeResourcesNames(datalakeResourcesNames);
        }
        return environmentResponses;
    }

    public DetailedEnvironmentV4Response get(String environmentName, Long workspaceId) {
        try {
            return transactionService.required(() ->
                    conversionService.convert(getByNameForWorkspaceId(environmentName, workspaceId), DetailedEnvironmentV4Response.class));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Set<Environment> findByNamesInWorkspace(Set<String> names, @NotNull Long workspaceId) {
        return CollectionUtils.isEmpty(names) ? new HashSet<>() : environmentRepository.findAllByNameInAndWorkspaceId(names, workspaceId);
    }

    public SimpleEnvironmentV4Response delete(String environmentName, Long workspaceId) {
        Environment environment = getByNameForWorkspaceId(environmentName, workspaceId);
        delete(environment);
        return conversionService.convert(environment, SimpleEnvironmentV4Response.class);
    }

    public DetailedEnvironmentV4Response createForLoggedInUser(EnvironmentV4Request request, @Nonnull Long workspaceId) {
        Environment environment = initEnvironment(request, workspaceId);
        CloudRegions cloudRegions = getCloudRegions(environment);
        setLocation(environment, request.getLocation(), cloudRegions);
        ValidationResult validationResult;
        if (cloudRegions.areRegionsSupported()) {
            setRegions(environment, request.getRegions(), cloudRegions);
        }
        validationResult = environmentCreationValidator.validate(environment, request, cloudRegions);
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        environment = createForLoggedInUser(environment, workspaceId);
        return conversionService.convert(environment, DetailedEnvironmentV4Response.class);
    }

    private Environment initEnvironment(EnvironmentV4Request request, @Nonnull Long workspaceId) {
        Environment environment = conversionService.convert(request, Environment.class);
        environment.setLdapConfigs(ldapConfigService.findByNamesInWorkspace(request.getLdaps(), workspaceId));
        environment.setProxyConfigs(proxyConfigService.findByNamesInWorkspace(request.getProxies(), workspaceId));
        environment.setRdsConfigs(rdsConfigService.findByNamesInWorkspace(request.getDatabases(), workspaceId));
        environment.setKubernetesConfigs(kubernetesConfigService.findByNamesInWorkspace(request.getKubernetes(), workspaceId));
        environment.setKerberosConfigs(kerberosService.findByNamesInWorkspace(request.getKerberoses(), workspaceId));
        Credential credential = environmentCredentialOperationService.getCredentialFromRequest(request, workspaceId);
        environment.setCredential(credential);
        environment.setCloudPlatform(credential.cloudPlatform());
        return environment;
    }

    private CloudRegions getCloudRegions(Environment environment) {
        PlatformResourceRequest platformResourceRequest = new PlatformResourceRequest();
        platformResourceRequest.setCredential(environment.getCredential());
        platformResourceRequest.setCloudPlatform(environment.getCloudPlatform());
        return platformParameterService.getRegionsByCredential(platformResourceRequest);
    }

    public DetailedEnvironmentV4Response edit(Long workspaceId, String environmentName, EnvironmentEditV4Request request) {
        Environment environment = getByNameForWorkspaceId(environmentName, workspaceId);
        if (StringUtils.isNotEmpty(request.getDescription())) {
            environment.setDescription(request.getDescription());
        }
        CloudRegions cloudRegions = getCloudRegions(environment);
        if (locationAndRegionChanged(request)) {
            editRegionsAndLocation(request, environment, cloudRegions);
        } else if (locationChanged(request)) {
            editLocation(request, environment, cloudRegions);
        } else if (CollectionUtils.isNotEmpty(request.getRegions())) {
            LocationV4Request locationRequest = conversionService.convert(environment, LocationV4Request.class);
            request.setLocation(locationRequest);
            editRegions(request, environment, cloudRegions);
        }
        environment = pureSave(environment);
        return conversionService.convert(environment, DetailedEnvironmentV4Response.class);
    }

    private boolean locationAndRegionChanged(EnvironmentEditV4Request request) {
        return CollectionUtils.isNotEmpty(request.getRegions())
                && locationChanged(request);
    }

    private boolean locationChanged(EnvironmentEditV4Request request) {
        return request.getLocation() != null && !request.getLocation().isEmpty();
    }

    private void editRegionsAndLocation(EnvironmentEditV4Request request, Environment environment, CloudRegions cloudRegions) {
        validateRegionAndLocation(request.getLocation(), request.getRegions(), cloudRegions, environment);
        setLocation(environment, request.getLocation(), cloudRegions);
        setRegions(environment, request.getRegions(), cloudRegions);
    }

    private void validateRegionAndLocation(LocationV4Request location, Set<String> requestedRegions,
            CloudRegions cloudRegions, Environment environment) {
        ValidationResultBuilder validationResultBuilder = environmentRegionValidator
                .validateRegions(requestedRegions, cloudRegions, environment.getCloudPlatform(), ValidationResult.builder());
        environmentRegionValidator.validateLocation(location, requestedRegions, environment, validationResultBuilder);
        ValidationResult validationResult = validationResultBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private void editLocation(EnvironmentEditV4Request request, Environment environment, CloudRegions cloudRegions) {
        Set<String> regions = environment.getRegionSet().stream()
                .map(Region::getName).collect(Collectors.toSet());
        validateRegionAndLocation(request.getLocation(), regions, cloudRegions, environment);
        setLocation(environment, request.getLocation(), cloudRegions);
    }

    private void editRegions(EnvironmentEditV4Request request, Environment environment, CloudRegions cloudRegions) {
        LocationV4Request locationRequest = conversionService.convert(environment, LocationV4Request.class);
        validateRegionAndLocation(locationRequest, request.getRegions(), cloudRegions, environment);
        setRegions(environment, request.getRegions(), cloudRegions);
    }

    private void setRegions(Environment environment, Set<String> requestedRegions, CloudRegions cloudRegions) {
        Set<Region> regionSet = new HashSet<>();
        Map<com.sequenceiq.cloudbreak.cloud.model.Region, String> displayNames = cloudRegions.getDisplayNames();
        for (com.sequenceiq.cloudbreak.cloud.model.Region r : cloudRegions.getCloudRegions().keySet()) {
            if (requestedRegions.contains(r.getRegionName())) {
                Region region = new Region();
                region.setName(r.getRegionName());
                String displayName = displayNames.get(r);
                region.setDisplayName(isEmpty(displayName) ? r.getRegionName() : displayName);
                regionSet.add(region);
            }
        }
        environment.setRegions(regionSet);
    }

    private void setLocation(Environment environment, LocationV4Request requestedLocation, CloudRegions cloudRegions) {
        if (requestedLocation != null) {
            Coordinate coordinate = cloudRegions.getCoordinates().get(region(requestedLocation.getName()));
            if (coordinate != null) {
                environment.setLocation(requestedLocation.getName());
                environment.setLocationDisplayName(coordinate.getDisplayName());
                environment.setLatitude(coordinate.getLatitude());
                environment.setLongitude(coordinate.getLongitude());
            } else if (requestedLocation.getLatitude() != null && requestedLocation.getLongitude() != null) {
                environment.setLocation(requestedLocation.getName());
                environment.setLocationDisplayName(requestedLocation.getName());
                environment.setLatitude(requestedLocation.getLatitude());
                environment.setLongitude(requestedLocation.getLongitude());
            } else {
                throw new BadRequestException(String.format("No location found with name %s in the location list. The supported locations are: [%s]",
                        requestedLocation, cloudRegions.locationNames()));
            }
        }
    }

    public DetailedEnvironmentV4Response attachResources(String environmentName, EnvironmentAttachV4Request request, Long workspaceId) {
        try {
            return transactionService.required(() -> {
                Set<LdapConfig> ldapsToAttach = ldapConfigService.findByNamesInWorkspace(request.getLdaps(), workspaceId);
                Set<ProxyConfig> proxiesToAttach = proxyConfigService.findByNamesInWorkspace(request.getProxies(), workspaceId);
                Set<RDSConfig> rdssToAttach = rdsConfigService.findByNamesInWorkspace(request.getDatabases(), workspaceId);
                Set<KubernetesConfig> kubesToAttach = kubernetesConfigService.findByNamesInWorkspace(request.getKubernetes(), workspaceId);
                ValidationResult validationResult = environmentAttachValidator.validate(request, ldapsToAttach, proxiesToAttach, rdssToAttach);
                Set<KerberosConfig> kerberosConfigsToAttach = kerberosService.findByNamesInWorkspace(request.getKerberoses(), workspaceId);
                if (validationResult.hasError()) {
                    throw new BadRequestException(validationResult.getFormattedErrors());
                }
                Environment environment = getByNameForWorkspaceId(environmentName, workspaceId);
                environment = doAttach(ldapsToAttach, proxiesToAttach, rdssToAttach, kubesToAttach, kerberosConfigsToAttach, environment);
                return conversionService.convert(environment, DetailedEnvironmentV4Response.class);
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private Environment doAttach(Set<LdapConfig> ldapsToAttach, Set<ProxyConfig> proxiesToAttach, Set<RDSConfig> rdssToAttach,
            Set<KubernetesConfig> kubesToAttach, Set<KerberosConfig> kerberosConfigs, Environment environment) {
        ldapsToAttach.removeAll(environment.getLdapConfigs());
        environment.getLdapConfigs().addAll(ldapsToAttach);
        proxiesToAttach.removeAll(environment.getProxyConfigs());
        environment.getProxyConfigs().addAll(proxiesToAttach);
        rdssToAttach.removeAll(environment.getRdsConfigs());
        environment.getRdsConfigs().addAll(rdssToAttach);
        kubesToAttach.removeAll(environment.getKubernetesConfigs());
        environment.getKubernetesConfigs().addAll(kubesToAttach);
        kerberosConfigs.removeAll(environment.getKerberosConfigs());
        environment.getKerberosConfigs().addAll(kerberosConfigs);
        environment = environmentRepository.save(environment);
        return environment;
    }

    public DetailedEnvironmentV4Response detachResources(String environmentName, EnvironmentDetachV4Request request, Long workspaceId) {
        try {
            return transactionService.required(() -> {
                Environment environment = getByNameForWorkspaceId(environmentName, workspaceId);
                ValidationResult validationResult = validateAndDetachLdaps(request, environment);
                validationResult = validateAndDetachProxies(request, environment, validationResult);
                validationResult = validateAndDetachRdss(request, environment, validationResult);
                validationResult = validateAndDetachKerberosConfigs(request, environment, validationResult);
                detachKubes(request, environment);
                if (validationResult.hasError()) {
                    throw new BadRequestException(validationResult.getFormattedErrors());
                }
                Environment saved = environmentRepository.save(environment);
                return conversionService.convert(saved, DetailedEnvironmentV4Response.class);
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private ValidationResult validateAndDetachLdaps(EnvironmentDetachV4Request request, Environment environment) {
        Set<LdapConfig> ldapsToDetach = environment.getLdapConfigs().stream()
                .filter(ldap -> request.getLdaps().contains(ldap.getName())).collect(Collectors.toSet());
        Map<LdapConfig, Set<Cluster>> ldapsToClusters = ldapsToDetach.stream()
                .collect(Collectors.toMap(ldap -> ldap, ldap -> ldapConfigService.getClustersUsingResourceInEnvironment(ldap, environment.getId())));
        ValidationResult validationResult = environmentDetachValidator.validate(environment, ldapsToClusters);
        environment.getLdapConfigs().removeAll(ldapsToDetach);
        return validationResult;
    }

    private ValidationResult validateAndDetachProxies(EnvironmentDetachV4Request request, Environment environment, ValidationResult validationResult) {
        Set<ProxyConfig> proxiesToDetach = environment.getProxyConfigs().stream()
                .filter(proxy -> request.getProxies().contains(proxy.getName())).collect(Collectors.toSet());
        Map<ProxyConfig, Set<Cluster>> proxiesToClusters = proxiesToDetach.stream()
                .collect(Collectors.toMap(proxy -> proxy, proxy -> proxyConfigService.getClustersUsingResourceInEnvironment(proxy, environment.getId())));
        validationResult = environmentDetachValidator.validate(environment, proxiesToClusters).merge(validationResult);
        environment.getProxyConfigs().removeAll(proxiesToDetach);
        return validationResult;
    }

    private ValidationResult validateAndDetachRdss(EnvironmentDetachV4Request request, Environment environment, ValidationResult validationResult) {
        Set<RDSConfig> rdssToDetach = environment.getRdsConfigs().stream()
                .filter(rds -> request.getDatabases().contains(rds.getName())).collect(Collectors.toSet());
        Map<RDSConfig, Set<Cluster>> rdssToClusters = rdssToDetach.stream()
                .collect(Collectors.toMap(rds -> rds, rds -> rdsConfigService.getClustersUsingResourceInEnvironment(rds, environment.getId())));
        validationResult = environmentDetachValidator.validate(environment, rdssToClusters).merge(validationResult);
        environment.getRdsConfigs().removeAll(rdssToDetach);
        return validationResult;
    }

    private void detachKubes(EnvironmentDetachV4Request request, Environment environment) {
        Set<KubernetesConfig> kubesToDetach = environment.getKubernetesConfigs().stream()
                .filter(config -> request.getKubernetes().contains(config.getName())).collect(Collectors.toSet());
        environment.getKubernetesConfigs().removeAll(kubesToDetach);
    }

    private ValidationResult validateAndDetachKerberosConfigs(EnvironmentDetachV4Request request, Environment environment, ValidationResult validationResult) {
        Set<KerberosConfig> kerberosConfigsToDetach = environment.getKerberosConfigs().stream()
                .filter(kerberosConfig -> request.getKerberoses().contains(kerberosConfig.getName())).collect(Collectors.toSet());
        Map<KerberosConfig, Set<Cluster>> kerberosConfigsToClusters = kerberosConfigsToDetach.stream()
                .collect(Collectors.toMap(kerberosConfig -> kerberosConfig, kerberosConfig ->
                        kerberosService.getClustersUsingResourceInEnvironment(kerberosConfig, environment.getId())));
        validationResult = environmentDetachValidator.validate(environment, kerberosConfigsToClusters).merge(validationResult);
        environment.getKerberosConfigs().removeAll(kerberosConfigsToDetach);
        return validationResult;
    }

    public DetailedEnvironmentV4Response changeCredential(String environmentName, Long workspaceId, EnvironmentChangeCredentialV4Request request) {
        try {
            return transactionService.required(() -> {
                Environment environment = getByNameForWorkspaceId(environmentName, workspaceId);
                Credential credential = environmentCredentialOperationService.validatePlatformAndGetCredential(request, environment, workspaceId);
                Set<StackApiView> stacksCannotBeChanged = environment.getStacks().stream()
                        .filter(stackApiView -> !stackApiViewService.canChangeCredential(stackApiView))
                        .collect(Collectors.toSet());
                if (stacksCannotBeChanged.isEmpty()) {
                    environment.setCredential(credential);
                    environment.getStacks().forEach(stackApiView -> {
                        stackApiView.setCredential(credential);
                        stackApiViewService.save(stackApiView);
                    });
                    return conversionService.convert(environmentRepository.save(environment), DetailedEnvironmentV4Response.class);
                } else {
                    throw new BadRequestException(String.format("Credential cannot be changed due to clusters with ongoing operation "
                            + "or not being in AVAILABLE state. Clusters: [%s].", stacksCannotBeChanged.stream()
                            .map(StackApiView::getName).collect(Collectors.joining(", "))));
                }
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public DetailedEnvironmentV4Response registerExternalDatalake(String environmentName, Long workspaceId, RegisterDatalakeV4Request registerDatalakeRequest) {
        try {
            return transactionService.required(() -> {
                try {
                    Environment environment = getByNameForWorkspaceId(environmentName, workspaceId);
                    EnvironmentView envView = environmentViewService.getByNameForWorkspaceId(environmentName, workspaceId);
                    ValidationResult validationResult = clusterCreationEnvironmentValidator.validate(registerDatalakeRequest, environment);
                    if (validationResult.hasError()) {
                        throw new BadRequestException(validationResult.getFormattedErrors());
                    }
                    Credential credential = environment.getCredential();
                    String attributesStr = credential.getAttributes();
                    Map<String, Object> attributes = isEmpty(attributesStr) ? new HashMap<>() : new Json(attributesStr).getMap();
                    String datalakeAmbariUrl = (String) attributes.get(CredentialPrerequisiteService.CUMULUS_AMBARI_URL);
                    String datalakeAmbariUser = (String) attributes.get(CredentialPrerequisiteService.CUMULUS_AMBARI_USER);
                    String datalakeAmbariPassowrd = (String) attributes.get(CredentialPrerequisiteService.CUMULUS_AMBARI_PASSWORD);
                    LdapConfig ldapConfig = isEmpty(registerDatalakeRequest.getLdapName()) ? null
                            : ldapConfigService.getByNameForWorkspaceId(registerDatalakeRequest.getLdapName(), workspaceId);
                    KerberosConfig kerberosConfig = isEmpty(registerDatalakeRequest.getKerberosName()) ? null
                            : kerberosService.getByNameForWorkspaceId(registerDatalakeRequest.getKerberosName(), workspaceId);
                    Set<RDSConfig> rdssConfigs = CollectionUtils.isEmpty(registerDatalakeRequest.getDatabaseNames()) ? null
                            : rdsConfigService.findByNamesInWorkspace(registerDatalakeRequest.getDatabaseNames(), workspaceId);
                    URL ambariUrl = new URL(datalakeAmbariUrl);
                    AmbariClient ambariClient = ambariClientProvider.getAmbariClient(ambariUrl, datalakeAmbariUser, datalakeAmbariPassowrd);
                    Map<String, Map<String, String>> serviceSecretParamMap = isEmpty(registerDatalakeRequest.getRangerAdminPassword())
                            ? new HashMap<>() : Map.ofEntries(Map.entry(ServiceDescriptorDefinitionProvider.RANGER_SERVICE, Map.ofEntries(
                            Map.entry(ServiceDescriptorDefinitionProvider.RANGER_ADMIN_PWD_KEY, registerDatalakeRequest.getRangerAdminPassword()))));
                    DatalakeResources datalakeResources = ambariDatalakeConfigProvider.collectAndStoreDatalakeResources(environmentName, envView,
                            datalakeAmbariUrl, ambariUrl.getHost(), ambariUrl.getHost(), ambariClient, serviceSecretParamMap, ldapConfig, kerberosConfig,
                            rdssConfigs, environment.getWorkspace());
                    environment.getDatalakeResources().add(datalakeResources);
                    return conversionService.convert(environmentRepository.save(environment), DetailedEnvironmentV4Response.class);
                } catch (MalformedURLException ex) {
                    throw new CloudbreakServiceException("", ex);
                }
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Optional<Environment> findById(Long id) {
        return environmentRepository.findById(id);
    }

    @Override
    protected void prepareDeletion(Environment environment) {
        Long aliveEnvs = stackService.countAliveByEnvironment(environment);
        if (aliveEnvs > 0) {
            throw new BadRequestException("Cannot delete environment. "
                    + "All clusters must be terminated before environment deletion. Alive clusters: " + aliveEnvs);
        }
    }

    @Override
    protected WorkspaceResourceRepository<Environment, Long> repository() {
        return environmentRepository;
    }

    @Override
    protected void prepareCreation(Environment resource) {
    }

    @Override
    public WorkspaceResource resource() {
        return ENVIRONMENT;
    }
}