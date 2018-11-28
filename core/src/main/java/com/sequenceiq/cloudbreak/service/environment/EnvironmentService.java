package com.sequenceiq.cloudbreak.service.environment;

import static com.sequenceiq.cloudbreak.authorization.WorkspaceResource.ENVIRONMENT;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentAttachRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentChangeCredentialRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentDetachRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.LocationRequest;
import com.sequenceiq.cloudbreak.api.model.environment.response.DetailedEnvironmentResponse;
import com.sequenceiq.cloudbreak.api.model.environment.response.SimpleEnvironmentResponse;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.Coordinate;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.environment.EnvironmentAttachValidator;
import com.sequenceiq.cloudbreak.controller.validation.environment.EnvironmentCreationValidator;
import com.sequenceiq.cloudbreak.controller.validation.environment.EnvironmentDetachValidator;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.KubernetesConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.PlatformResourceRequest;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.domain.environment.Region;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.repository.environment.EnvironmentRepository;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.KubernetesConfigService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.platform.PlatformParameterService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;
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
    private EnvironmentCredentialOperationService environmentCredentialOperationService;

    @Inject
    private EnvironmentCreationValidator environmentCreationValidator;

    @Inject
    private EnvironmentAttachValidator environmentAttachValidator;

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
    private CloudParameterCache cloudParameterCache;

    @Inject
    private EnvironmentDetachValidator environmentDetachValidator;

    public Set<SimpleEnvironmentResponse> listByWorkspaceId(Long workspaceId) {
        return environmentViewService.findAllByWorkspaceId(workspaceId).stream()
                .map(env -> conversionService.convert(env, SimpleEnvironmentResponse.class))
                .peek(env -> {
                    Set<String> datalakeNames = stackService.findDatalakeStackNamesByWorkspaceAndEnvironment(workspaceId, env.getId());
                    env.setDatalakeClusterNames(datalakeNames);
                    Set<String> workloadNames = stackService.findWorkloadStackNamesByWorkspaceAndEnvironment(workspaceId, env.getId());
                    env.setWorkloadClusterNames(workloadNames);
                })
                .collect(Collectors.toSet());
    }

    public DetailedEnvironmentResponse get(String environmentName, Long workspaceId) {
        try {
            return transactionService.required(() ->
                    conversionService.convert(getByNameForWorkspaceId(environmentName, workspaceId), DetailedEnvironmentResponse.class));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Set<Environment> findByNamesInWorkspace(Set<String> names, @NotNull Long workspaceId) {
        return CollectionUtils.isEmpty(names) ? new HashSet<>() : environmentRepository.findAllByNameInAndWorkspaceId(names, workspaceId);
    }

    @Override
    protected void prepareDeletion(Environment environment) {
        Long alive = stackService.countAliveByEnvironment(environment);
        if (alive > 0) {
            throw new BadRequestException("Cannot delete environment. "
                    + "All clusters must be terminated before environment deletion. Alive clusters: " + alive);
        }
    }

    public SimpleEnvironmentResponse delete(String environmentName, Long workspaceId) {
        Environment environment = getByNameForWorkspaceId(environmentName, workspaceId);
        delete(environment);
        return conversionService.convert(environment, SimpleEnvironmentResponse.class);
    }

    public DetailedEnvironmentResponse createForLoggedInUser(EnvironmentRequest request, @Nonnull Long workspaceId) {
        Environment environment = conversionService.convert(request, Environment.class);
        environment.setLdapConfigs(ldapConfigService.findByNamesInWorkspace(request.getLdapConfigs(), workspaceId));
        environment.setProxyConfigs(proxyConfigService.findByNamesInWorkspace(request.getProxyConfigs(), workspaceId));
        environment.setRdsConfigs(rdsConfigService.findByNamesInWorkspace(request.getRdsConfigs(), workspaceId));
        environment.setKubernetesConfigs(kubernetesConfigService.findByNamesInWorkspace(request.getRdsConfigs(), workspaceId));
        Credential credential = environmentCredentialOperationService.getCredentialFromRequest(request, workspaceId);
        environment.setCredential(credential);
        environment.setCloudPlatform(credential.cloudPlatform());
        boolean regionsSupported = cloudParameterCache.areRegionsSupported(environment.getCloudPlatform());
        setRegions(environment, request, regionsSupported);
        ValidationResult validationResult = environmentCreationValidator.validate(environment, request, regionsSupported);
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        environment = createForLoggedInUser(environment, workspaceId);
        return conversionService.convert(environment, DetailedEnvironmentResponse.class);
    }

    private void setRegions(Environment environment, EnvironmentRequest environmentRequest, boolean regionsSupported) {
        try {
            PlatformResourceRequest platformResourceRequest = new PlatformResourceRequest();
            platformResourceRequest.setCredential(environment.getCredential());
            platformResourceRequest.setCloudPlatform(environment.getCloudPlatform());
            CloudRegions cloudRegions = platformParameterService.getRegionsByCredential(platformResourceRequest);
            if (regionsSupported) {
                Set<Region> regionSet = new HashSet<>();
                Map<com.sequenceiq.cloudbreak.cloud.model.Region, String> displayNames = cloudRegions.getDisplayNames();
                for (com.sequenceiq.cloudbreak.cloud.model.Region r : cloudRegions.getCloudRegions().keySet()) {
                    if (environmentRequest.getRegions().contains(r.getRegionName())) {
                        Region region = new Region();
                        region.setName(r.getRegionName());
                        String displayName = displayNames.get(r);
                        region.setDisplayName(StringUtils.isEmpty(displayName) ? r.getRegionName() : displayName);
                        regionSet.add(region);
                    }
                }
                environment.setRegions(new Json(regionSet));
            } else {
                environment.setRegions(new Json(new HashSet<>()));
            }
            setLocation(environment, environmentRequest, cloudRegions);

        } catch (JsonProcessingException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    private void setLocation(Environment environment, EnvironmentRequest environmentRequest, CloudRegions cloudRegions) {
        Coordinate coordinate = cloudRegions.getCoordinates().get(region(environmentRequest.getLocation().getLocationName()));
        LocationRequest location = environmentRequest.getLocation();
        if (coordinate != null) {
            environment.setLocation(coordinate.getDisplayName());
            environment.setLatitude(coordinate.getLatitude());
            environment.setLongitude(coordinate.getLongitude());
        } else if (location != null && location.getLatitude() != null && location.getLongitude() != null) {
            environment.setLocation(location.getLocationName());
            environment.setLatitude(location.getLatitude());
            environment.setLongitude(location.getLongitude());
        } else {
            throw new BadRequestException(String.format("No location found with name %s in the location list. The supported locations are: [%s]",
                    environmentRequest.getLocation(), cloudRegions.locationNames()));
        }
    }

    public DetailedEnvironmentResponse attachResources(String environmentName, EnvironmentAttachRequest request, Long workspaceId) {
        try {
            return transactionService.required(() -> {
                Set<LdapConfig> ldapsToAttach = ldapConfigService.findByNamesInWorkspace(request.getLdapConfigs(), workspaceId);
                Set<ProxyConfig> proxiesToAttach = proxyConfigService.findByNamesInWorkspace(request.getProxyConfigs(), workspaceId);
                Set<RDSConfig> rdssToAttach = rdsConfigService.findByNamesInWorkspace(request.getRdsConfigs(), workspaceId);
                Set<KubernetesConfig> kubesToAttach = kubernetesConfigService.findByNamesInWorkspace(request.getKubernetesConfigs(), workspaceId);
                ValidationResult validationResult = environmentAttachValidator.validate(request, ldapsToAttach, proxiesToAttach, rdssToAttach);
                if (validationResult.hasError()) {
                    throw new BadRequestException(validationResult.getFormattedErrors());
                }
                Environment environment = getByNameForWorkspaceId(environmentName, workspaceId);
                environment = doAttach(ldapsToAttach, proxiesToAttach, rdssToAttach, kubesToAttach, environment);
                return conversionService.convert(environment, DetailedEnvironmentResponse.class);
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private Environment doAttach(Set<LdapConfig> ldapsToAttach, Set<ProxyConfig> proxiesToAttach, Set<RDSConfig> rdssToAttach,
            Set<KubernetesConfig> kubesToAttach, Environment environment) {
        ldapsToAttach.removeAll(environment.getLdapConfigs());
        environment.getLdapConfigs().addAll(ldapsToAttach);
        proxiesToAttach.removeAll(environment.getProxyConfigs());
        environment.getProxyConfigs().addAll(proxiesToAttach);
        rdssToAttach.removeAll(environment.getRdsConfigs());
        environment.getRdsConfigs().addAll(rdssToAttach);
        kubesToAttach.removeAll(environment.getKubernetesConfigs());
        environment.getKubernetesConfigs().addAll(kubesToAttach);
        environment = environmentRepository.save(environment);
        return environment;
    }

    public DetailedEnvironmentResponse detachResources(String environmentName, EnvironmentDetachRequest request, Long workspaceId) {
        try {
            return transactionService.required(() -> {
                Environment environment = getByNameForWorkspaceId(environmentName, workspaceId);
                ValidationResult validationResult = validateAndDetachLdaps(request, environment);
                validationResult = validateAndDetachProxies(request, environment, validationResult);
                validationResult = validateAndDetachRdss(request, environment, validationResult);
                detachKubes(request, environment);
                if (validationResult.hasError()) {
                    throw new BadRequestException(validationResult.getFormattedErrors());
                }
                Environment saved = environmentRepository.save(environment);
                return conversionService.convert(saved, DetailedEnvironmentResponse.class);
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private ValidationResult validateAndDetachLdaps(EnvironmentDetachRequest request, Environment environment) {
        Set<LdapConfig> ldapsToDetach = environment.getLdapConfigs().stream()
                .filter(ldap -> request.getLdapConfigs().contains(ldap.getName())).collect(Collectors.toSet());
        Map<LdapConfig, Set<Cluster>> ldapsToClusters = ldapsToDetach.stream()
                .collect(Collectors.toMap(ldap -> ldap, ldap -> ldapConfigService.getClustersUsingResourceInEnvironment(ldap, environment.getId())));
        ValidationResult validationResult = environmentDetachValidator.validate(environment, ldapsToClusters);
        environment.getLdapConfigs().removeAll(ldapsToDetach);
        return validationResult;
    }

    private ValidationResult validateAndDetachProxies(EnvironmentDetachRequest request, Environment environment, ValidationResult validationResult) {
        Set<ProxyConfig> proxiesToDetach = environment.getProxyConfigs().stream()
                .filter(proxy -> request.getProxyConfigs().contains(proxy.getName())).collect(Collectors.toSet());
        Map<ProxyConfig, Set<Cluster>> proxiesToClusters = proxiesToDetach.stream()
                .collect(Collectors.toMap(proxy -> proxy, proxy -> proxyConfigService.getClustersUsingResourceInEnvironment(proxy, environment.getId())));
        validationResult = environmentDetachValidator.validate(environment, proxiesToClusters).merge(validationResult);
        environment.getProxyConfigs().removeAll(proxiesToDetach);
        return validationResult;
    }

    private ValidationResult validateAndDetachRdss(EnvironmentDetachRequest request, Environment environment, ValidationResult validationResult) {
        Set<RDSConfig> rdssToDetach = environment.getRdsConfigs().stream()
                .filter(rds -> request.getRdsConfigs().contains(rds.getName())).collect(Collectors.toSet());
        Map<RDSConfig, Set<Cluster>> rdssToClusters = rdssToDetach.stream()
                .collect(Collectors.toMap(rds -> rds, rds -> rdsConfigService.getClustersUsingResourceInEnvironment(rds, environment.getId())));
        validationResult = environmentDetachValidator.validate(environment, rdssToClusters).merge(validationResult);
        environment.getRdsConfigs().removeAll(rdssToDetach);
        return validationResult;
    }

    private void detachKubes(EnvironmentDetachRequest request, Environment environment) {
        Set<KubernetesConfig> kubesToDetach = environment.getKubernetesConfigs().stream()
                .filter(config -> request.getKubernetesConfigs().contains(config.getName())).collect(Collectors.toSet());
        environment.getKubernetesConfigs().removeAll(kubesToDetach);
    }

    public DetailedEnvironmentResponse changeCredential(String environmentName, Long workspaceId, EnvironmentChangeCredentialRequest request) {
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
                    return conversionService.convert(environmentRepository.save(environment), DetailedEnvironmentResponse.class);
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
