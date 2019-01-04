package com.sequenceiq.cloudbreak.reactor.handler.ldap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.ldap.LdapSSOConfigurationFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.ldap.LdapSSOConfigurationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.ldap.LdapSSOConfigurationSuccess;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariLdapService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariRepositoryVersionService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariSSOService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class LdapSSOConfigurationHandler implements ReactorEventHandler<LdapSSOConfigurationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapSSOConfigurationHandler.class);

    @Inject
    private AmbariLdapService ambariLdapService;

    @Inject
    private AmbariSSOService ambariSSOService;

    @Inject
    private StackService stackService;

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private AmbariRepositoryVersionService ambariRepositoryVersionService;

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
                if (ambariRepositoryVersionService.setupLdapAndSsoOnApi(ambariRepo)) {
                    LOGGER.debug("Setup LDAP and SSO on API");
                    ambariLdapService.setupLdap(stack, stack.getCluster(), ambariRepo);
                    ambariLdapService.syncLdap(stack, stack.getCluster());
                    ambariSSOService.setupSSO(stack, stack.getCluster());
                } else {
                    LOGGER.debug("Can not setup LDAP and SSO on API, Ambari too old");
                }
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
