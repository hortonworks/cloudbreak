package com.sequenceiq.environment.environment.flow.upgrade.ccm.handler;

import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmHandlerSelectors.UPGRADE_CCM_TUNNEL_UPDATE_HANDLER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmEvent;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmFailedEvent;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class UpgradeCcmTunnelUpdateHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmTunnelUpdateHandler.class);

    private final EnvironmentService environmentService;

    protected UpgradeCcmTunnelUpdateHandler(EventSender eventSender, EnvironmentService environmentService) {
        super(eventSender);
        this.environmentService = environmentService;
    }

    @Override
    public String selector() {
        return UPGRADE_CCM_TUNNEL_UPDATE_HANDLER.name();
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        LOGGER.debug("In UpgradeCcmTunnelUpdateHandler.accept");
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            environmentService.updateTunnelByEnvironmentId(environmentDto.getResourceId(), Tunnel.latestUpgradeTarget());

            UpgradeCcmEvent upgradeCcmEvent = UpgradeCcmEvent.builder()
                    .withSelector(UpgradeCcmStateSelectors.UPGRADE_CCM_DATALAKE_EVENT.selector())
                    .withResourceCrn(environmentDto.getResourceCrn())
                    .withResourceId(environmentDto.getId())
                    .withResourceName(environmentDto.getName())
                    .build();

            eventSender().sendEvent(upgradeCcmEvent, environmentDtoEvent.getHeaders());
            LOGGER.debug("UPGRADE_CCM_DATALAKE_EVENT event sent");
        } catch (Exception e) {
            UpgradeCcmFailedEvent failedEvent = new UpgradeCcmFailedEvent(environmentDto, e, EnvironmentStatus.UPGRADE_CCM_TUNNEL_UPDATE_FAILED);
            eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
            LOGGER.debug("UPGRADE_CCM_TUNNEL_UPDATE_FAILED event sent");
        }
    }
}
