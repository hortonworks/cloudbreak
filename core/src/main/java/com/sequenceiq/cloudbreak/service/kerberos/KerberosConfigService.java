package com.sequenceiq.cloudbreak.service.kerberos;

import static java.lang.String.format;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.repository.KerberosConfigRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;

public class KerberosConfigService extends AbstractWorkspaceAwareResourceService<KerberosConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosConfigService.class);

    @Inject
    private KerberosConfigRepository repository;

    @Inject
    private ClusterService clusterService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    protected WorkspaceResourceRepository<KerberosConfig, Long> repository() {
        return repository;
    }

    @Override
    protected void prepareDeletion(KerberosConfig resource) {
    }

    @Override
    protected void prepareCreation(KerberosConfig resource) {
        repository().findByNameAndWorkspaceId(resource.getName(), restRequestThreadLocalService.getRequestedWorkspaceId())
                .ifPresent(kerberosConfig -> {
                    String message = format("KerberosConfig – in the given workspace – with name [%s] is already exists", resource.getName());
                    LOGGER.info(message);
                    throw new BadRequestException(message);
                });
    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.KERBEROS_CONFIG;
    }

    public KerberosConfig save(KerberosConfig kerberosConfig) {
        return repository.save(kerberosConfig);
    }
}
