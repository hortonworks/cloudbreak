package com.sequenceiq.cloudbreak.reactor.handler.ldap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.ldap.LdapSSOConfigurationFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.ldap.LdapSSOConfigurationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.ldap.LdapSSOConfigurationSuccess;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class LdapSSOConfigurationHandler implements ReactorEventHandler<LdapSSOConfigurationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapSSOConfigurationHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(LdapSSOConfigurationRequest.class);
    }

    @Override
    public void accept(Event<LdapSSOConfigurationRequest> ldapConfigurationRequestEvent) {
        Long stackId = ldapConfigurationRequestEvent.getData().getStackId();
        Selectable response;
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            AmbariRepo ambariRepo = clusterComponentConfigProvider.getAmbariRepo(stack.getCluster().getId());
            if (ambariRepo != null) {
                GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
                clusterApiConnectors.getConnector(stack)
                        .clusterSecurityService().setupLdapAndSSO(ambariRepo, primaryGatewayConfig.getPublicAddress());
            } else {
                LOGGER.debug("Can not setup LDAP and SSO on API, because Ambari repo is not found");
            }
            response = new LdapSSOConfigurationSuccess(stackId);
        } catch (RuntimeException e) {
            LOGGER.info("Error during LDAP configuration, stackId: " + stackId, e);
            response = new LdapSSOConfigurationFailed(stackId, e);
        }
        eventBus.notify(response.selector(), new Event<>(ldapConfigurationRequestEvent.getHeaders(), response));
    }
}
