package com.sequenceiq.it.cloudbreak.util;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.it.cloudbreak.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.dto.AbstractTestDto;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class FlowUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowUtil.class);

    private FlowUtil() {
    }

    public static <R, S, T extends CloudbreakTestDto, U extends MicroserviceClient> void setFlow(String marker, AbstractTestDto<R, S, T, U> testDto,
            FlowIdentifier flowIdentifier, SdxClient client) {
        if (flowIdentifier != null) {
            switch (flowIdentifier.getType()) {
                case FLOW:
                    Log.when(LOGGER, format(" %s flow %s started ", marker, flowIdentifier.getPollableId()));
                    testDto.setLastKnownFlowId(flowIdentifier.getPollableId());
                    break;
                case FLOW_CHAIN:
                    Log.when(LOGGER, format(" %s flow chain %s started ", marker, flowIdentifier.getPollableId()));
                    testDto.setLastKnownFlowChainId(flowIdentifier.getPollableId());
                    break;
                default:
                    setFlowBasedOnFlowApi(marker, testDto, client);
            }
        } else {
            setFlowBasedOnFlowApi(marker, testDto, client);
        }
    }

    private static <R, S, T extends CloudbreakTestDto, U extends MicroserviceClient> void setFlowBasedOnFlowApi(String marker,
            AbstractTestDto<R, S, T, U> testDto, SdxClient client) {
        FlowLogResponse lastFlowByResourceName = client.getSdxClient()
                .flowEndpoint()
                .getLastFlowByResourceName(testDto.getName());
        Log.when(LOGGER, format(" Get %s flow id %s and flow chain id %s", marker, lastFlowByResourceName.getFlowId(),
                lastFlowByResourceName.getFlowChainId()));
        testDto.setLastKnownFlowChainId(lastFlowByResourceName.getFlowChainId());
        testDto.setLastKnownFlowId(lastFlowByResourceName.getFlowId());
    }
}
