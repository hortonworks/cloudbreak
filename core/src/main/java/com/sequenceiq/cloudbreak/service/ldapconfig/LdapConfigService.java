package com.sequenceiq.cloudbreak.service.ldapconfig;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests.LdapMinimalV4Request;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ldapconfig.LdapConfigValidator;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.LdapConfigRepository;
import com.sequenceiq.cloudbreak.repository.environment.EnvironmentResourceRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.environment.AbstractEnvironmentAwareService;

@Service
public class LdapConfigService extends AbstractEnvironmentAwareService<LdapConfig> {

    @Inject
    private LdapConfigRepository ldapConfigRepository;

    @Inject
    private ClusterService clusterService;

    @Inject
    private LdapConfigValidator ldapConfigValidator;

    public LdapConfig get(Long id) {
        return ldapConfigRepository.findById(id).orElseThrow(notFound("LdapConfig", id));
    }

    public void delete(Long id) {
        delete(get(id));
    }

    @Override
    public EnvironmentResourceRepository<LdapConfig, Long> repository() {
        return ldapConfigRepository;
    }

    @Override
    public Set<Cluster> getClustersUsingResource(LdapConfig ldapConfig) {
        return clusterService.findByLdapConfig(ldapConfig);
    }

    @Override
    public Set<Cluster> getClustersUsingResourceInEnvironment(LdapConfig ldapConfig, Long environmentId) {
        return clusterService.findAllClustersByLdapConfigInEnvironment(ldapConfig, environmentId);
    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.LDAP;
    }

    public String testConnection(Long workspaceId, String existingName, LdapMinimalV4Request existingLdapConfig) {
        if (existingName == null && existingLdapConfig == null) {
            throw new BadRequestException("Either an existing resource 'name' or an LDAP 'validationRequest' needs to be specified in the request. ");
        }
        try {
            if (existingName != null) {
                LdapConfig ldapConfig = getByNameForWorkspaceId(existingName, workspaceId);
                ldapConfigValidator.validateLdapConnection(ldapConfig);
            } else {
                ldapConfigValidator.validateLdapConnection(existingLdapConfig);
            }
            return "connected";
        } catch (BadRequestException e) {
            return e.getMessage();
        }
    }
}
