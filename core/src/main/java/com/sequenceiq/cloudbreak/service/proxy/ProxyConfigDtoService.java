package com.sequenceiq.cloudbreak.service.proxy;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;

@Service
public class ProxyConfigDtoService {

    @Inject
    private ClusterService clusterService;

    @Inject
    private SecretService secretService;

    public ProxyConfig getByCrnAndAccountId(String resourceCrn, String accountId) {
        //TODO use Environment MS client to get Proxy resource and build a ProxyConfig DTO
//        secretService.getByResponse(proxyConfigResponse.getUserName());
//        secretService.getByResponse(proxyConfigResponse.getPassword());
        return null;
    }
}
