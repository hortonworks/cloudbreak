package com.sequenceiq.cloudbreak.reactor.handler.kerberos;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.KeytabModel;
import com.sequenceiq.cloudbreak.reactor.api.event.kerberos.KeytabConfigurationFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.kerberos.KeytabConfigurationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.kerberos.KeytabConfigurationSuccess;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.KerberosMgmtV1Endpoint;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.RoleRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabRequest;
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
    private KerberosMgmtV1Endpoint kerberosMgmtV1Endpoint;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private SecretService secretService;

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
            Optional<KerberosConfig> kerberosConfigOptional = kerberosConfigService.get(stack.getEnvironmentCrn());
            // TODO remove Cloudplatform check when FreeIPA registration is ready
            if ((CloudPlatform.AWS.name().equals(stack.cloudPlatform()) || CloudPlatform.AZURE.name().equals(stack.cloudPlatform()))
                    && kerberosConfigOptional.isPresent() && kerberosDetailService.isIpaJoinable(kerberosConfigOptional.get())) {
                GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
                ServiceKeytabResponse serviceKeytabResponse = getServiceKeytabResponse(stack, primaryGatewayConfig);
                KeytabModel keytabModel = buildKeytabModel(serviceKeytabResponse);
                hostOrchestrator.uploadKeytabs(List.of(primaryGatewayConfig), Set.of(keytabModel),
                        ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel(stackId, stack.getCluster().getId()));
            }
            response = new KeytabConfigurationSuccess(stackId);
        } catch (Exception e) {
            LOGGER.info("Error during keytab configuration, stackId: " + stackId, e);
            response = new KeytabConfigurationFailed(stackId, e);
        }
        eventBus.notify(response.selector(), new Event<>(keytabConfigurationRequestEvent.getHeaders(), response));
    }

    private KeytabModel buildKeytabModel(ServiceKeytabResponse serviceKeytabResponse) {
        String keytabInBase64 = secretService.getByResponse(serviceKeytabResponse.getKeytab());
        byte[] keytab = Base64.getDecoder().decode(keytabInBase64.getBytes(StandardCharsets.UTF_8));
        String principal = secretService.getByResponse(serviceKeytabResponse.getServicePrincipal());
        return new KeytabModel("CM", "/etc/cloudera-scm-server", "cmf.keytab", principal, keytab);
    }

    private ServiceKeytabResponse getServiceKeytabResponse(Stack stack, GatewayConfig primaryGatewayConfig)
            throws Exception {
        ServiceKeytabRequest request = new ServiceKeytabRequest();
        request.setEnvironmentCrn(stack.getEnvironmentCrn());
        request.setServerHostName(primaryGatewayConfig.getHostname());
        request.setServiceName("CM");
        request.setDoNotRecreateKeytab(Boolean.TRUE);
        RoleRequest roleRequest = new RoleRequest();
        roleRequest.setRoleName("hadoopadminrole-" + stack.getName());
        roleRequest.setPrivileges(Set.of("Service Administrators", "Certificate Administrators"));
        request.setRoleRequest(roleRequest);
        return kerberosMgmtV1Endpoint.generateServiceKeytab(request);
    }
}
