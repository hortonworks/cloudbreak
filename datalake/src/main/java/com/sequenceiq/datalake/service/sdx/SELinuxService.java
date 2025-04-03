package com.sequenceiq.datalake.service.sdx;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxEvent;
import com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxStateSelectors;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@Service
public class SELinuxService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VerticalScaleService.class);

    @Inject
    private EventSender eventSender;

    public FlowIdentifier enableSeLinuxOnDatalake(SdxCluster sdxCluster, String userCrn) {
        MDCBuilder.buildMdcContext(sdxCluster);
        LOGGER.info("Data Lake Cluster Enable SELinux flow triggered for DL {}", sdxCluster.getName());
        DatalakeEnableSeLinuxEvent datalakeEnableSeLinuxEvent = DatalakeEnableSeLinuxEvent.builder()
                .withAccepted(new Promise<>())
                .withResourceCrn(sdxCluster.getResourceCrn())
                .withResourceId(sdxCluster.getId())
                .withResourceName(sdxCluster.getName())
                .withSelector(DatalakeEnableSeLinuxStateSelectors.ENABLE_SELINUX_DATALAKE_EVENT.selector())
                .build();
        FlowIdentifier flowIdentifier = eventSender.sendEvent(datalakeEnableSeLinuxEvent, new Event.Headers(getFlowTriggerUsercrn(userCrn)));
        LOGGER.debug("Data Lake Cluster Vertical Scale flow trigger event sent for environment {}", sdxCluster.getName());
        return flowIdentifier;
    }

    private Map<String, Object> getFlowTriggerUsercrn(String userCrn) {
        return Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, userCrn);
    }
}
