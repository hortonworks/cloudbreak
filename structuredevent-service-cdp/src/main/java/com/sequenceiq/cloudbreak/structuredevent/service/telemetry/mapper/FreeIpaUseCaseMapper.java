package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;

@Component
public class FreeIpaUseCaseMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaUseCaseMapper.class);

    @Inject
    private CDPRequestProcessingStepMapper cdpRequestProcessingStepMapper;

    private Map<Pair, UsageProto.CDPFreeIPAStatus.Value> firstStepUseCaseMap;

    @VisibleForTesting
    @PostConstruct
    void initUseCaseMaps() {
        firstStepUseCaseMap = new HashMap<>();
        firstStepUseCaseMap.put(Pair.of("", "UpscaleFlowConfig"), UsageProto.CDPFreeIPAStatus.Value.UPSCALE_STARTED);
        firstStepUseCaseMap.put(Pair.of("", "DownscaleFlowConfig"), UsageProto.CDPFreeIPAStatus.Value.DOWNSCALE_STARTED);
    }

    public UsageProto.CDPFreeIPAStatus.Value useCase(FlowDetails flow) {
        UsageProto.CDPFreeIPAStatus.Value useCase = UsageProto.CDPFreeIPAStatus.Value.UNSET;
        if (flow != null) {
            String rootFlowChainType = defaultIfEmpty(flow.getFlowChainType(), "");
            if (cdpRequestProcessingStepMapper.isFirstStep(flow)) {
                useCase = firstStepToUseCaseMapping(rootFlowChainType, flow.getFlowType());
            } else if (cdpRequestProcessingStepMapper.isLastStep(flow)) {
                useCase = lastStepToUseCaseMapping(rootFlowChainType, flow.getFlowType(), flow.getNextFlowState());
            }
        }
        LOGGER.debug("FlowDetails: {}, Usecase: {}", flow, useCase);
        return useCase;
    }

    private UsageProto.CDPFreeIPAStatus.Value firstStepToUseCaseMapping(String rootFlowChainType, String flowType) {
        UsageProto.CDPFreeIPAStatus.Value useCase =
                firstStepUseCaseMap.getOrDefault(Pair.of(rootFlowChainType, flowType), UsageProto.CDPFreeIPAStatus.Value.UNSET);
        LOGGER.debug("Mapping flow type to use-case: [flowchain: {}, flow: {}]: usecase: {}", rootFlowChainType, flowType, useCase);
        return useCase;
    }

    //CHECKSTYLE:OFF: CyclomaticComplexity
    private UsageProto.CDPFreeIPAStatus.Value lastStepToUseCaseMapping(String rootFlowChainType, String flowType, String nextFlowState) {
        UsageProto.CDPFreeIPAStatus.Value useCase = UsageProto.CDPFreeIPAStatus.Value.UNSET;
        String rootFlowType = StringUtils.isNotEmpty(rootFlowChainType) ? rootFlowChainType : flowType;
        if (rootFlowType != null) {
            switch (rootFlowType) {
                case "UpscaleFlowConfig":
                    useCase = getFreeIpaStatus(nextFlowState, "UPSCALE_FINISHED_STATE",
                            UsageProto.CDPFreeIPAStatus.Value.UPSCALE_FINISHED,
                            UsageProto.CDPFreeIPAStatus.Value.UPSCALE_FAILED);
                    break;
                case "DownscaleFlowConfig":
                    useCase = getFreeIpaStatus(nextFlowState, "DOWNSCALE_FINISHED_STATE",
                            UsageProto.CDPFreeIPAStatus.Value.DOWNSCALE_FINISHED,
                            UsageProto.CDPFreeIPAStatus.Value.DOWNSCALE_FAILED);
                    break;
                default:
                    LOGGER.debug("Next flow state: {}", nextFlowState);
            }
        }
        LOGGER.debug("Mapping next flow state to use-case: [flowchain: {}, flow:{}, nextflowstate: {}]: {}",
                rootFlowChainType, flowType, nextFlowState, useCase);
        return useCase;
    }
    //CHECKSTYLE:ON

    private UsageProto.CDPFreeIPAStatus.Value getFreeIpaStatus(String nextFlowState, String finishedFlowState,
            UsageProto.CDPFreeIPAStatus.Value finishedStatus, UsageProto.CDPFreeIPAStatus.Value failedStatus) {
        if (nextFlowState.equals(finishedFlowState)) {
            return finishedStatus;
        } else if (nextFlowState.contains("_FAIL")) {
            return failedStatus;
        }
        return UsageProto.CDPFreeIPAStatus.Value.UNSET;
    }

}
