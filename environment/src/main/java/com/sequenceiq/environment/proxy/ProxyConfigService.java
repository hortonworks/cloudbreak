package com.sequenceiq.environment.proxy;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import org.springframework.stereotype.Service;

import com.sequenceiq.environment.environment.repository.EnvironmentResourceRepository;
import com.sequenceiq.environment.environment.service.AbstractEnvironmentAwareService;

@Service
public class ProxyConfigService extends AbstractEnvironmentAwareService<ProxyConfig> {

    private final ProxyConfigRepository proxyConfigRepository;

    public ProxyConfigService(ProxyConfigRepository proxyConfigRepository) {
        this.proxyConfigRepository = proxyConfigRepository;
    }

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

}
