package com.sequenceiq.cloudbreak.service.kerberos;

import static java.lang.String.format;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.KerberosConfigRepository;
import com.sequenceiq.cloudbreak.repository.environment.EnvironmentResourceRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.environment.AbstractEnvironmentAwareService;

@Service
public class KerberosService extends AbstractEnvironmentAwareService<KerberosConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosService.class);

    @Inject
    private KerberosConfigRepository kerberosConfigRepository;

    @Inject
    private ClusterService clusterService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    protected EnvironmentResourceRepository<KerberosConfig, Long> repository() {
        return kerberosConfigRepository;
    }

    @Override
    protected void prepareCreation(KerberosConfig resource) {
        Optional.ofNullable(repository().findByNameAndWorkspaceId(resource.getName(), restRequestThreadLocalService.getRequestedWorkspaceId()))
                .ifPresent(kerberosConfig -> {
                    LOGGER.info(format("KerberosConfig – in the given workspace – with name [%s] is already exists", resource.getName()));
                    throw new BadRequestException("KerberosConfig is already exists in the given workspace ");
                });
    }

    @Override
    public Set<Cluster> getClustersUsingResource(KerberosConfig resource) {
        return clusterService.findByKerberosConfig(resource.getId());
    }

    @Override
    public Set<Cluster> getClustersUsingResourceInEnvironment(KerberosConfig resource, Long environmentId) {
        return clusterService.findAllClustersByKerberosConfigInEnvironment(resource, environmentId);
    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.KERBEROS_CONFIG;
    }

}
