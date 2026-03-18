package com.sequenceiq.cloudbreak.reactor.handler.cluster.trustedrealm;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ServiceConfiguration;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.trustedrealm.UpdateTrustedRealmFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.trustedrealm.UpdateTrustedRealmRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.trustedrealm.UpdateTrustedRealmResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterServiceConfigurationLookup;
import com.sequenceiq.cloudbreak.service.cluster.ClusterServiceConfigurationUpdate;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class UpdateTrustedRealmHandler extends ExceptionCatcherEventHandler<UpdateTrustedRealmRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateTrustedRealmHandler.class);

    private static final String CORE_SETTINGS_SERVICE = "core_settings";

    private static final String TRUSTED_REALMS_CONFIG = "trusted_realms";

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterService clusterService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpdateTrustedRealmRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpdateTrustedRealmRequest> event) {
        return new UpdateTrustedRealmFailureEvent(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpdateTrustedRealmRequest> event) {
        UpdateTrustedRealmRequest request = event.getData();
        Long stackId = request.getResourceId();
        String realm = request.getRealm().toUpperCase(Locale.ROOT);
        LOGGER.info("Updating trusted realm '{}' on CM for stack {}", realm, stackId);

        StackDto stackDto = stackDtoService.getById(stackId);
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(stackDto.getResourceCrn());

        ClusterServiceConfigurationLookup configRequest = new ClusterServiceConfigurationLookup();
        configRequest.setServiceName(CORE_SETTINGS_SERVICE);
        configRequest.setConfigName(TRUSTED_REALMS_CONFIG);

        Optional<String> currentValue = clusterService.getClusterServiceConfigValue(nameOrCrn, configRequest);
        boolean realmAlreadyConfigured = currentValue.map(val -> val.contains(realm)).orElse(false);

        if (realmAlreadyConfigured) {
            LOGGER.info("Realm '{}' is already configured in trusted_realms for stack {}, skipping update", realm, stackId);
        } else {
            String newValue = currentValue.map(val -> val + "," + realm).orElse(realm);
            ServiceConfiguration trustedRealmsConfiguration = new ServiceConfiguration();
            trustedRealmsConfiguration.setServiceName(CORE_SETTINGS_SERVICE);
            trustedRealmsConfiguration.setConfigName(TRUSTED_REALMS_CONFIG);
            trustedRealmsConfiguration.setValue(newValue);
            ClusterServiceConfigurationUpdate updateRequest = new ClusterServiceConfigurationUpdate();
            updateRequest.setServiceConfigurations(List.of(trustedRealmsConfiguration));
            clusterService.updateClusterServiceConfiguration(nameOrCrn, updateRequest);
            LOGGER.info("Successfully updated trusted_realms to '{}' for stack {}", newValue, stackId);
        }

        return new UpdateTrustedRealmResult(stackId);
    }
}
