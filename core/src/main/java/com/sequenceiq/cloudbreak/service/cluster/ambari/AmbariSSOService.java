package com.sequenceiq.cloudbreak.service.cluster.ambari;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.template.views.GatewayView;

@Service
public class AmbariSSOService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariSSOService.class);

    @Value("${cb.knox.port}")
    private String knoxPort;

    @Inject
    private AmbariClientFactory clientFactory;

    @Inject
    private GatewayConfigService gatewayConfigService;

    public void setupSSO(Stack stack, Cluster cluster) {
        AmbariClient ambariClient = clientFactory.getAmbariClient(stack, cluster);
        Gateway gateway = cluster.getGateway();
        if (cluster.hasGateway() && SSOType.SSO_PROVIDER == cluster.getGateway().getSsoType()) {
            LOGGER.debug("Setup gateway on Ambari API for stack: {}", stack.getId());
            GatewayView gatewayView = new GatewayView(gateway, gateway.getSignKey());
            GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
            Map<String, Object> ssoConfigs = new HashMap<>();
            ssoConfigs.put("ambari.sso.provider.url", "https://" + primaryGatewayConfig.getPublicAddress() + ":" + knoxPort + gatewayView.getSsoProvider());
            ssoConfigs.put("ambari.sso.provider.certificate", gatewayView.getSignCert());
            ssoConfigs.put("ambari.sso.authentication.enabled", true);
            ssoConfigs.put("ambari.sso.manage_services", true);
            ssoConfigs.put("ambari.sso.enabled_services", "*");
            ssoConfigs.put("ambari.sso.jwt.cookieName", "hadoop-jwt");
            ambariClient.configureSSO(ssoConfigs);
        }
    }

}
