package com.sequenceiq.cloudbreak.reactor.handler.cluster.trustedrealm;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

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

        StackDto stackDto = stackDtoService.getById(stackId);
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(stackDto.getResourceCrn());

        ClusterServiceConfigurationLookup configRequest = new ClusterServiceConfigurationLookup();
        configRequest.setServiceName(CORE_SETTINGS_SERVICE);
        configRequest.setConfigName(TRUSTED_REALMS_CONFIG);

        Optional<String> currentValue = clusterService.getClusterServiceConfigValue(nameOrCrn, configRequest);

        if (request.isRemove()) {
            removeRealm(stackId, realm, nameOrCrn, currentValue);
        } else {
            addRealm(stackId, realm, nameOrCrn, currentValue);
        }

        return new UpdateTrustedRealmResult(stackId);
    }

    private void addRealm(Long stackId, String realm, NameOrCrn nameOrCrn, Optional<String> currentValue) {
        LOGGER.info("Adding trusted realm '{}' on CM for stack {}", realm, stackId);
        boolean realmAlreadyConfigured = currentValue.map(val -> Arrays.stream(val.split(","))
                .map(String::trim)
                .anyMatch(r -> r.equalsIgnoreCase(realm))).orElse(false);
        if (realmAlreadyConfigured) {
            LOGGER.info("Realm '{}' is already configured in trusted_realms for stack {}, skipping update", realm, stackId);
        } else {
            String newValue = currentValue.map(val -> val + "," + realm).orElse(realm);
            writeConfig(nameOrCrn, newValue);
            LOGGER.info("Successfully updated trusted_realms to '{}' for stack {}", newValue, stackId);
        }
    }

    private void removeRealm(Long stackId, String realm, NameOrCrn nameOrCrn, Optional<String> currentValue) {
        LOGGER.info("Removing trusted realm '{}' from CM for stack {}", realm, stackId);
        boolean realmPresent = currentValue.map(val -> Arrays.stream(val.split(","))
                .map(String::trim)
                .anyMatch(r -> r.equalsIgnoreCase(realm))).orElse(false);
        if (!realmPresent) {
            LOGGER.info("Realm '{}' is not present in trusted_realms for stack {}, skipping removal", realm, stackId);
        } else {
            String filtered = currentValue
                    .map(val -> Arrays.stream(val.split(","))
                            .map(String::trim)
                            .filter(r -> !r.equalsIgnoreCase(realm))
                            .collect(Collectors.joining(",")))
                    .orElse("");
            // Pass null when no realms remain so CM reverts the config to its default value instead of storing an empty string.
            String updatedValue = filtered.isEmpty() ? null : filtered;
            writeConfig(nameOrCrn, updatedValue);
            LOGGER.info("Successfully updated trusted_realms to '{}' for stack {}", updatedValue, stackId);
        }
    }

    private void writeConfig(NameOrCrn nameOrCrn, String value) {
        ServiceConfiguration trustedRealmsConfiguration = new ServiceConfiguration();
        trustedRealmsConfiguration.setServiceName(CORE_SETTINGS_SERVICE);
        trustedRealmsConfiguration.setConfigName(TRUSTED_REALMS_CONFIG);
        trustedRealmsConfiguration.setValue(value);
        ClusterServiceConfigurationUpdate updateRequest = new ClusterServiceConfigurationUpdate();
        updateRequest.setServiceConfigurations(List.of(trustedRealmsConfiguration));
        clusterService.updateClusterServiceConfiguration(nameOrCrn, updateRequest);
    }
}
