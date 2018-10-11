package com.sequenceiq.cloudbreak.service.proxy;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.environment.EnvironmentResourceRepository;
import com.sequenceiq.cloudbreak.repository.ProxyConfigRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.environment.AbstractEnvironmentAwareService;

@Service
public class ProxyConfigService extends AbstractEnvironmentAwareService<ProxyConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigService.class);

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
    public EnvironmentResourceRepository<ProxyConfig, Long> repository() {
        return proxyConfigRepository;
    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.PROXY;
    }

    @Override
    protected void prepareDeletion(ProxyConfig resource) {
        if (resource == null) {
            throw new NotFoundException("Proxy config not found.");
        }
        Set<Cluster> clustersWithThisProxy = clusterService.findByProxyConfig(resource);
        if (!clustersWithThisProxy.isEmpty()) {
            if (clustersWithThisProxy.size() > 1) {
                String clusters = clustersWithThisProxy
                        .stream()
                        .map(Cluster::getName)
                        .collect(Collectors.joining(", "));
                throw new BadRequestException(String.format(
                        "There are clusters associated with proxy config '%s'. Please remove these before deleting the proxy configuration. "
                                + "The following clusters are using this proxy configuration: [%s]", resource.getName(), clusters));
            }
            throw new BadRequestException(String.format("There is a cluster ['%s'] which uses proxy configuration '%s'. Please remove this "
                    + "cluster before deleting the proxy configuration", clustersWithThisProxy.iterator().next().getName(), resource.getName()));
        }
    }

    @Override
    protected void prepareCreation(ProxyConfig resource) {

    }
}
