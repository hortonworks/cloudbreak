package com.sequenceiq.cloudbreak.service.proxy;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@Service
public class ProxyConfigDtoService {

    @Inject
    private ClusterService clusterService;

    public ProxyConfig getByCrnAndAccountId(String resourceCrn, String accountId) {
        //TODO use Environment MS client to get Proxy resource and transform secrets to simple values
        return null;
    }
}
