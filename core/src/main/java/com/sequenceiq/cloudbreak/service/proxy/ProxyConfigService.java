package com.sequenceiq.cloudbreak.service.proxy;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.repository.ProxyConfigRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;

@Service
public class ProxyConfigService extends AbstractWorkspaceAwareResourceService<ProxyConfig> {

    @Inject
    private ProxyConfigRepository proxyConfigRepository;

    @Inject
    private ClusterService clusterService;

    public ProxyConfig get(Long id) {
        return proxyConfigRepository.findById(id).orElseThrow(notFound("Proxy configuration", id));
    }

    public ProxyConfig delete(Long id) {
        return delete(get(id));
    }

    @Override
    protected WorkspaceResourceRepository<ProxyConfig, Long> repository() {
        return proxyConfigRepository;
    }

    @Override
    protected void prepareDeletion(ProxyConfig resource) {
        Set<Cluster> clustersWithThisProxy = clusterService.findByProxyConfig(resource);
        if (!clustersWithThisProxy.isEmpty()) {
            String clusters = clustersWithThisProxy
                    .stream()
                    .map(Cluster::getName)
                    .collect(Collectors.joining(", "));
            throw new BadRequestException(String.format(resource().getReadableName() + " '%s' cannot be deleted"
                    + " because there are clusters associated with it: [%s].", resource.getName(), clusters));
        }
    }

    @Override
    protected void prepareCreation(ProxyConfig resource) {

    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.PROXY;
    }

}
