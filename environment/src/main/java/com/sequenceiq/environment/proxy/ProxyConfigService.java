package com.sequenceiq.environment.proxy;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;
import com.sequenceiq.environment.environment.EnvironmentResourceRepository;
import com.sequenceiq.environment.environment.AbstractEnvironmentAwareService;

@Service
public class ProxyConfigService extends AbstractEnvironmentAwareService<ProxyConfig> {

    @Inject
    private ProxyConfigRepository proxyConfigRepository;

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

}
