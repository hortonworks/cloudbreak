package com.sequenceiq.cloudbreak.service.environment;

import static com.sequenceiq.cloudbreak.authorization.WorkspaceResource.ENVIRONMENT;

import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentAttachRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentDetachRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentRequest;
import com.sequenceiq.cloudbreak.api.model.environment.response.DetailedEnvironmentResponse;
import com.sequenceiq.cloudbreak.api.model.environment.response.SimpleEnvironmentResponse;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.environment.EnvironmentAttachValidator;
import com.sequenceiq.cloudbreak.controller.validation.environment.EnvironmentCreationValidator;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.repository.environment.EnvironmentRepository;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.fn.tuple.Tuple;

@Service
public class EnvironmentService extends AbstractWorkspaceAwareResourceService<Environment> {

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private ProxyConfigService proxyConfigService;

    @Inject
    private CredentialService credentialService;

    @Inject
    private EnvironmentCreationValidator environmentCreationValidator;

    @Inject
    private EnvironmentAttachValidator environmentAttachValidator;

    @Inject
    private EnvironmentViewService environmentViewService;

    @Inject
    private StackService stackService;

    @Inject
    private EnvironmentRepository environmentRepository;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

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
        setCredential(request, environment, workspaceId);
        ValidationResult validationResult = environmentCreationValidator.validate(Tuple.of(environment, request));
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        environment = createForLoggedInUser(environment, workspaceId);
        return conversionService.convert(environment, DetailedEnvironmentResponse.class);
    }

    private void setCredential(EnvironmentRequest request, Environment environment, Long workspaceId) {
        Credential credential;
        if (StringUtils.isNotEmpty(request.getCredentialName())) {
            try {
                credential = credentialService.getByNameForWorkspaceId(request.getCredentialName(), workspaceId);
            } catch (NotFoundException e) {
                throw new BadRequestException(String.format("No credential found with name [%s] in the workspace.",
                        request.getCredentialName()), e);
            }
        } else {
            Credential converted = conversionService.convert(request.getCredential(), Credential.class);
            credential = credentialService.createForLoggedInUser(converted, workspaceId);
        }
        environment.setCredential(credential);
        environment.setCloudPlatform(credential.cloudPlatform());
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
