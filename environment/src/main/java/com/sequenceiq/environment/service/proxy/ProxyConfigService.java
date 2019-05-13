package com.sequenceiq.environment.service.proxy;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;
import com.sequenceiq.environment.domain.proxy.ProxyConfig;
import com.sequenceiq.environment.repository.environment.EnvironmentResourceRepository;
import com.sequenceiq.environment.repository.proxy.ProxyConfigRepository;
import com.sequenceiq.environment.service.environment.AbstractEnvironmentAwareService;

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
