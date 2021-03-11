package com.sequenceiq.cloudbreak.reactor.handler.kerberos;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.KeytabModel;
import com.sequenceiq.cloudbreak.reactor.api.event.kerberos.KeytabConfigurationException;
import com.sequenceiq.cloudbreak.reactor.api.event.kerberos.KeytabConfigurationFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.kerberos.KeytabConfigurationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.kerberos.KeytabConfigurationSuccess;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentConfigProvider;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabResponse;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class KeytabConfigurationHandler implements EventHandler<KeytabConfigurationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeytabConfigurationHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private EventBus eventBus;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private KerberosDetailService kerberosDetailService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private SecretService secretService;

    @Inject
    private KeytabProvider keytabProvider;

    @Inject
    private EnvironmentConfigProvider environmentConfigProvider;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(KeytabConfigurationRequest.class);
    }

    @Override
    public void accept(Event<KeytabConfigurationRequest> keytabConfigurationRequestEvent) {
        Long stackId = keytabConfigurationRequestEvent.getData().getResourceId();
        Selectable response;
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            Optional<KerberosConfig> kerberosConfigOptional = kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName());
            boolean childEnvironment = environmentConfigProvider.isChildEnvironment(stack.getEnvironmentCrn());

            //if needed here make it so that we call getserivcekeytab on each gateway.
            if (kerberosDetailService.keytabsShouldBeUpdated(stack.cloudPlatform(), childEnvironment, kerberosConfigOptional)) {
                GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
                ServiceKeytabResponse serviceKeytabResponse = keytabProvider.getServiceKeytabResponse(stack, primaryGatewayConfig);
                KeytabModel keytabModel = buildKeytabModel(serviceKeytabResponse);
                hostOrchestrator.uploadKeytabs(gatewayConfigService.getAllGatewayConfigs(stack), Set.of(keytabModel),
                        ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel(stackId, stack.getCluster().getId()));
            }
            response = new KeytabConfigurationSuccess(stackId);
        } catch (Exception e) {
            LOGGER.info("Error during keytab configuration, stackId: " + stackId, e);
            KeytabConfigurationException configurationException = new KeytabConfigurationException("Keytab generation failed with: " + e.getMessage(), e);
            response = new KeytabConfigurationFailed(stackId, configurationException);
        }
        eventBus.notify(response.selector(), new Event<>(keytabConfigurationRequestEvent.getHeaders(), response));
    }

    private KeytabModel buildKeytabModel(ServiceKeytabResponse serviceKeytabResponse) {
        String keytabInBase64 = secretService.getByResponse(serviceKeytabResponse.getKeytab());
        byte[] keytab = Base64.getDecoder().decode(keytabInBase64.getBytes(StandardCharsets.UTF_8));
        String principal = secretService.getByResponse(serviceKeytabResponse.getServicePrincipal());
        return new KeytabModel("CM", "/etc/cloudera-scm-server", "cmf.keytab", principal, keytab);
    }
}
