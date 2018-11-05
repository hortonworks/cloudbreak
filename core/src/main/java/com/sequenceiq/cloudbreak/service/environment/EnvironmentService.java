package com.sequenceiq.cloudbreak.service.environment;

import static com.sequenceiq.cloudbreak.authorization.WorkspaceResource.ENVIRONMENT;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentAttachRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentChangeCredentialRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentDetachRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentRequest;
import com.sequenceiq.cloudbreak.api.model.environment.response.DetailedEnvironmentResponse;
import com.sequenceiq.cloudbreak.api.model.environment.response.SimpleEnvironmentResponse;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.environment.EnvironmentAttachValidator;
import com.sequenceiq.cloudbreak.controller.validation.environment.EnvironmentCreationValidator;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.PlatformResourceRequest;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.domain.environment.Region;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.repository.environment.EnvironmentRepository;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
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

    public Set<SimpleEnvironmentResponse> listByWorkspaceId(Long workspaceId) {
        return environmentViewService.findAllByWorkspaceId(workspaceId).stream()
                .map(env -> conversionService.convert(env, SimpleEnvironmentResponse.class))
                .collect(Collectors.toSet());
    }

    public DetailedEnvironmentResponse get(String environmentName, Long workspaceId) {
        return conversionService.convert(getByNameForWorkspaceId(environmentName, workspaceId), DetailedEnvironmentResponse.class);
    }

    @Override
    protected void prepareDeletion(Environment environment) {
        Long alive = stackService.countAliveByEnvironment(environment);
        if (alive > 0) {
            throw new BadRequestException("Cannot delete environment. "
                    + "All clusters must be terminated before environment deletion. Alive clusters: " + alive);
        }
    }

    public DetailedEnvironmentResponse delete(String environmentName, Long workspaceId) {
        Environment environment = getByNameForWorkspaceId(environmentName, workspaceId);
        delete(environment);
        return conversionService.convert(environment, DetailedEnvironmentResponse.class);
    }

    public DetailedEnvironmentResponse createForLoggedInUser(EnvironmentRequest request, @Nonnull Long workspaceId) {
        Environment environment = conversionService.convert(request, Environment.class);
        environment.setLdapConfigs(ldapConfigService.findByNamesInWorkspace(request.getLdapConfigs(), workspaceId));
        environment.setProxyConfigs(proxyConfigService.findByNamesInWorkspace(request.getProxyConfigs(), workspaceId));
        environment.setRdsConfigs(rdsConfigService.findByNamesInWorkspace(request.getRdsConfigs(), workspaceId));
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
            if (regionsSupported) {
                PlatformResourceRequest platformResourceRequest = new PlatformResourceRequest();
                platformResourceRequest.setCredential(environment.getCredential());
                platformResourceRequest.setCloudPlatform(environment.getCloudPlatform());
                CloudRegions cloudRegions = platformParameterService.getRegionsByCredential(platformResourceRequest);
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
        } catch (JsonProcessingException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    public DetailedEnvironmentResponse attachResources(String environmentName, EnvironmentAttachRequest request, Long workspaceId) {
        Set<LdapConfig> ldapsToAttach = ldapConfigService.findByNamesInWorkspace(request.getLdapConfigs(), workspaceId);
        Set<ProxyConfig> proxiesToAttach = proxyConfigService.findByNamesInWorkspace(request.getProxyConfigs(), workspaceId);
        Set<RDSConfig> rdssToAttach = rdsConfigService.findByNamesInWorkspace(request.getRdsConfigs(), workspaceId);
        ValidationResult validationResult = environmentAttachValidator.validate(request, ldapsToAttach, proxiesToAttach, rdssToAttach);
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        Environment environment = getByNameForWorkspaceId(environmentName, workspaceId);
        environment = doAttach(ldapsToAttach, proxiesToAttach, rdssToAttach, environment);
        return conversionService.convert(environment, DetailedEnvironmentResponse.class);
    }

    private Environment doAttach(Set<LdapConfig> ldapsToAttach, Set<ProxyConfig> proxiesToAttach, Set<RDSConfig> rdssToAttach, Environment environment) {
        ldapsToAttach.removeAll(environment.getLdapConfigs());
        environment.getLdapConfigs().addAll(ldapsToAttach);
        proxiesToAttach.removeAll(environment.getProxyConfigs());
        environment.getProxyConfigs().addAll(proxiesToAttach);
        rdssToAttach.removeAll(environment.getRdsConfigs());
        environment.getRdsConfigs().addAll(rdssToAttach);
        environment = environmentRepository.save(environment);
        return environment;
    }

    public DetailedEnvironmentResponse detachResources(String environmentName, EnvironmentDetachRequest request, Long workspaceId) {
        Environment environment = getByNameForWorkspaceId(environmentName, workspaceId);
        Set<LdapConfig> ldapsToRemove = environment.getLdapConfigs().stream()
                .filter(ldap -> request.getLdapConfigs().contains(ldap.getName())).collect(Collectors.toSet());
        environment.getLdapConfigs().removeAll(ldapsToRemove);
        Set<ProxyConfig> proxiesToRemove = environment.getProxyConfigs().stream()
                .filter(proxy -> request.getProxyConfigs().contains(proxy.getName())).collect(Collectors.toSet());
        environment.getProxyConfigs().removeAll(proxiesToRemove);
        Set<RDSConfig> rdssToRemove = environment.getRdsConfigs().stream()
                .filter(rds -> request.getRdsConfigs().contains(rds.getName())).collect(Collectors.toSet());
        environment.getRdsConfigs().removeAll(rdssToRemove);
        environment = environmentRepository.save(environment);
        return conversionService.convert(environment, DetailedEnvironmentResponse.class);
    }

    public DetailedEnvironmentResponse changeCredential(String environmentName, Long workspaceId, EnvironmentChangeCredentialRequest request) {
        try {
            return transactionService.required(() -> {
                Environment environment = getByNameForWorkspaceId(environmentName, workspaceId);
                Credential credential = environmentCredentialOperationService.validatePlatformAndGetCredential(request, environment, workspaceId);
                Set<StackApiView> stacksCannotBeChanged = environment.getWorkloadClusters().stream()
                        .filter(stackApiView -> !stackApiViewService.canChangeCredential(stackApiView))
                        .collect(Collectors.toSet());
                if (stacksCannotBeChanged.isEmpty()) {
                    environment.setCredential(credential);
                    environment.getWorkloadClusters().forEach(stackApiView -> {
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
