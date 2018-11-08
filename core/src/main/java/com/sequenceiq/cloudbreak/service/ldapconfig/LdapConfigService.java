package com.sequenceiq.cloudbreak.service.ldapconfig;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
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

    @Override
    protected void prepareCreation(LdapConfig resource) {
    }
}
