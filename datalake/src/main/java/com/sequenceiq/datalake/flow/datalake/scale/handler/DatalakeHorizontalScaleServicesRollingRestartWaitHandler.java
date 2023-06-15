package com.sequenceiq.datalake.flow.datalake.scale.handler;

import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleEvent.DATALAKE_HORIZONTAL_SCALE_FINISHED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleHandlerEvent.DATALAKE_HORIZONTAL_SCALE_CM_ROLLING_RESTART_HANDLER;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.datalake.scale.event.DatalakeHorizontalScaleFlowEvent;
import com.sequenceiq.datalake.flow.datalake.scale.event.DatalakeHorizontalScaleFlowEvent.DatalakeHorizontalScaleFlowEventBuilder;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class DatalakeHorizontalScaleServicesRollingRestartWaitHandler extends EventSenderAwareHandler<DatalakeHorizontalScaleFlowEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeHorizontalScaleServicesRollingRestartWaitHandler.class);

    @Value("${sdx.datalake.scale.sleeptime-sec:10}")
    private int sleepTimeInSec;

    @Value("${sdx.datalake.scale.duration-min:30}")
    private int durationInMinutes;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private CloudbreakPoller cloudbreakPoller;

    @Inject
    private SdxService sdxService;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    protected DatalakeHorizontalScaleServicesRollingRestartWaitHandler(EventSender eventSender) {
        super(eventSender);
    }

    @Override
    public String selector() {
        return DATALAKE_HORIZONTAL_SCALE_CM_ROLLING_RESTART_HANDLER.selector();
    }

    @Override
    public void accept(Event<DatalakeHorizontalScaleFlowEvent> event) {
        DatalakeHorizontalScaleFlowEvent data = event.getData();
        SdxCluster sdxCluster = sdxService.getById(data.getResourceId());
        FlowIdentifier flowId = stackV4Endpoint.rollingRestartServices(0L, data.getResourceCrn());
        cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowId);
        PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES)
                .withStopPollingIfExceptionOccurred(true);
        cloudbreakPoller.pollFlowStateBySdxClusterUntilComplete("Datalake horizontal scaling",
                sdxCluster, pollingConfig);
        LOGGER.debug("Services Rolling restart finsihed");
        DatalakeHorizontalScaleFlowEventBuilder resultEventBuilder = DatalakeHorizontalScaleFlowEvent
                .datalakeHorizontalScaleFlowEventBuilderFactory(data)
                .setSelector(DATALAKE_HORIZONTAL_SCALE_FINISHED_EVENT.selector());
        eventSender().sendEvent(resultEventBuilder.build(), event.getHeaders());
    }
}
