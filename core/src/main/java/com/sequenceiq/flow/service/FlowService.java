package com.sequenceiq.flow.service;

import static com.google.common.base.Preconditions.checkState;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

@Service
public class FlowService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowService.class);

    @Inject
    private FlowLogDBService flowLogDBService;

    @Inject
    private FlowChainLogService flowChainLogService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    public FlowLogResponse getLastFlowById(String flowId) {
        LOGGER.info("Getting last flow log by flow id {}", flowId);
        Optional<FlowLog> lastFlowLog = flowLogDBService.getLastFlowLog(flowId);
        if (lastFlowLog.isPresent()) {
            return conversionService.convert(lastFlowLog.get(), FlowLogResponse.class);
        }
        throw new BadRequestException("Not found flow for this flow id!");
    }

    public List<FlowLogResponse> getFlowLogsByFlowId(String flowId) {
        LOGGER.info("Getting flow logs by flow id {}", flowId);
        List<FlowLog> flowLogs = flowLogDBService.findAllByFlowIdOrderByCreatedDesc(flowId);
        return flowLogs.stream().map(flowLog -> conversionService.convert(flowLog, FlowLogResponse.class)).collect(Collectors.toList());
    }

    public FlowLogResponse getLastFlowByResourceName(String resourceName) {
        checkState(!Crn.isCrn(resourceName));
        LOGGER.info("Getting last flow log by resource name {}", resourceName);
        return conversionService.convert(flowLogDBService.getLastFlowLogByResourceCrnOrName(resourceName), FlowLogResponse.class);
    }

    public FlowLogResponse getLastFlowByResourceCrn(String resourceCrn) {
        checkState(Crn.isCrn(resourceCrn));
        LOGGER.info("Getting last flow log by resource crn {}", resourceCrn);
        return conversionService.convert(flowLogDBService.getLastFlowLogByResourceCrnOrName(resourceCrn), FlowLogResponse.class);
    }

    public List<FlowLogResponse> getFlowLogsByResourceCrn(String resourceCrn) {
        checkState(Crn.isCrn(resourceCrn));
        LOGGER.info("Getting flow logs by resource crn {}", resourceCrn);
        List<FlowLog> flowLogs = flowLogDBService.getFlowLogsByResourceCrnOrName(resourceCrn);
        return flowLogs.stream().map(flowLog -> conversionService.convert(flowLog, FlowLogResponse.class)).collect(Collectors.toList());
    }

    public List<FlowLogResponse> getFlowLogsByResourceName(String resourceName) {
        checkState(!Crn.isCrn(resourceName));
        LOGGER.info("Getting flow logs by resource name {}", resourceName);
        List<FlowLog> flowLogs = flowLogDBService.getFlowLogsByResourceCrnOrName(resourceName);
        return flowLogs.stream().map(flowLog -> conversionService.convert(flowLog, FlowLogResponse.class)).collect(Collectors.toList());
    }

    public List<FlowLogResponse> getFlowLogsByResourceNameAndChainId(String resourceName, String chainId) {
        List<FlowChainLog> flowChains = flowChainLogService.findByFlowChainIdOrderByCreatedDesc(chainId);
        if (!flowChains.isEmpty()) {
            LOGGER.info("Getting flow logs by resource name {} and flow chain id {}", resourceName, chainId);
            Set<FlowChainLog> relatedChains = getRelatedFlowChainLogs(flowChains);
            List<String> relatedChainIds = relatedChains.stream().map(flowChainLog -> flowChainLog.getFlowChainId()).collect(Collectors.toList());
            List<FlowLog> flowLogs = flowLogDBService.getFlowLogsByResourceAndChainId(resourceName, relatedChainIds);
            return flowLogs.stream().map(flowLog -> conversionService.convert(flowLog, FlowLogResponse.class)).collect(Collectors.toList());
        }
        throw new NotFoundException("FlowChain not found by this flowChainId!");
    }

    public FlowCheckResponse hasFlowRunning(String resourceName, String chainId) {
        List<FlowChainLog> flowChains = flowChainLogService.findByFlowChainIdOrderByCreatedDesc(chainId);
        if (!flowChains.isEmpty()) {
            LOGGER.info("Checking if there is an active flow based on resource name {} and flow chain id {}", resourceName, chainId);
            Set<FlowChainLog> relatedChains = getRelatedFlowChainLogs(flowChains);
            List<String> relatedChainIds = relatedChains.stream().map(flowChainLog -> flowChainLog.getFlowChainId()).collect(Collectors.toList());
            List<FlowLog> relatedFlowLogs = flowLogDBService.getFlowLogsByResourceAndChainId(resourceName, relatedChainIds);
            FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
            flowCheckResponse.setFlowChainId(chainId);
            flowCheckResponse.setHasActiveFlow(flowChainLogService.checkIfAnyFlowChainHasEventInQueue(relatedChains) ||
                    flowLogDBService.hasPendingFlowEvent(relatedFlowLogs));
            return flowCheckResponse;
        }
        throw new NotFoundException("FlowChain not found by this flowChainId!");
    }

    private Set<FlowChainLog> getRelatedFlowChainLogs(List<FlowChainLog> sourceFlowChains) {
        Optional<FlowChainLog> flowChainWithParent = sourceFlowChains.stream()
                .filter(flowChainLog -> StringUtils.isNotBlank(flowChainLog.getParentFlowChainId())).findFirst();
        FlowChainLog lastFlowChain = sourceFlowChains.stream().sorted(Comparator.comparing(FlowChainLog::getCreated).reversed()).findFirst().get();
        FlowChainLog inputFlowChain = flowChainWithParent.isPresent() ? flowChainWithParent.get() : lastFlowChain;
        return flowChainLogService.collectRelatedFlowChains(inputFlowChain);
    }
}
