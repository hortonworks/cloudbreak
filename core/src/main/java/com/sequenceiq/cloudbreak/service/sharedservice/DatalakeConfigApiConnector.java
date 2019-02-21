package com.sequenceiq.cloudbreak.service.sharedservice;

import java.net.URL;

import javax.inject.Inject;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.api.DatalakeConfigApi;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;

@Service
public class DatalakeConfigApiConnector {

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private TlsSecurityService tlsSecurityService;

    public DatalakeConfigApi getConnector(URL ambariUrl, String ambariUserName, String ambariPassword) {
        return (DatalakeConfigApi) applicationContext.getBean(DatalakeConfigApi.CUMULUS, ambariUrl, ambariUserName, ambariPassword);
    }

    public DatalakeConfigApi getConnector(Stack stack) {
        HttpClientConfig httpClientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), stack.getAmbariIp());
        return (DatalakeConfigApi) applicationContext.getBean(DatalakeConfigApi.CUMULUS, stack, httpClientConfig);
    }
}
