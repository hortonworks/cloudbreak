package com.sequenceiq.flow.controller;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.BadRequestException;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.core.ResourceIdProvider;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

@Controller
public class FlowController implements FlowEndpoint {

    @Inject
    private FlowLogDBService flowLogDBService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private ResourceIdProvider resourceIdProvider;

    @Override
    public FlowLogResponse getLastFlowById(String flowId) {
        Optional<FlowLog> lastFlowLog = flowLogDBService.getLastFlowLog(flowId);
        if (lastFlowLog.isPresent()) {
            return conversionService.convert(lastFlowLog.get(), FlowLogResponse.class);
        }
        throw new BadRequestException("Not found flow for this flow id!");
    }

    @Override
    public List<FlowLogResponse> getFlowLogsByFlowId(String flowId) {
        List<FlowLog> flowLogs = flowLogDBService.findAllByFlowIdOrderByCreatedDesc(flowId);
        return flowLogs.stream().map(flowLog -> conversionService.convert(flowLog, FlowLogResponse.class)).collect(Collectors.toList());
    }

    @Override
    public FlowLogResponse getLastFlowByResourceName(String resourceName) {
        checkState(!Crn.isCrn(resourceName));
        return conversionService.convert(flowLogDBService.getLastFlowLogByResourceCrnOrName(resourceName), FlowLogResponse.class);
    }

    @Override
    public FlowLogResponse getLastFlowByResourceCrn(String resourceCrn) {
        checkState(Crn.isCrn(resourceCrn));
        return conversionService.convert(flowLogDBService.getLastFlowLogByResourceCrnOrName(resourceCrn), FlowLogResponse.class);
    }

    @Override
    public List<FlowLogResponse> getFlowLogsByResourceCrn(String resourceCrn) {
        checkState(Crn.isCrn(resourceCrn));
        List<FlowLog> flowLogs = flowLogDBService.getFlowLogsByResourceCrnOrName(resourceCrn);
        return flowLogs.stream().map(flowLog -> conversionService.convert(flowLog, FlowLogResponse.class)).collect(Collectors.toList());
    }

    @Override
    public List<FlowLogResponse> getFlowLogsByResourceName(String resourceName) {
        checkState(!Crn.isCrn(resourceName));
        List<FlowLog> flowLogs = flowLogDBService.getFlowLogsByResourceCrnOrName(resourceName);
        return flowLogs.stream().map(flowLog -> conversionService.convert(flowLog, FlowLogResponse.class)).collect(Collectors.toList());
    }
}
