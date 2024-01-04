package com.sequenceiq.flow.service;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.flow.api.model.FlowProgressResponse;
import com.sequenceiq.flow.converter.FlowProgressResponseConverter;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

@Service
public class FlowProgressService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowProgressService.class);

    @Inject
    private FlowLogDBService flowLogDBService;

    @Inject
    private FlowProgressResponseConverter flowProgressResponseConverter;

    public FlowProgressResponse getLastFlowProgressByResourceCrn(String resourceCrn) {
        checkState(Crn.isCrn(resourceCrn));
        LOGGER.info("Getting flow logs (progress) by resource crn {}", resourceCrn);
        List<FlowLog> flowLogs = flowLogDBService.getFlowLogsByResourceCrnOrName(resourceCrn);
        FlowProgressResponse response = flowProgressResponseConverter.convert(flowLogs, resourceCrn);
        if (StringUtils.isBlank(response.getFlowId())) {
            throw new NotFoundException(String.format("Not found any historical flow data for requested resource (crn: %s)", resourceCrn));
        }
        return flowProgressResponseConverter.convert(flowLogs, resourceCrn);
    }

    public List<FlowProgressResponse> getFlowProgressListByResourceCrn(String resourceCrn) {
        checkState(Crn.isCrn(resourceCrn));
        LOGGER.info("Getting flow logs (progress) for all recent flows by resource crn {}", resourceCrn);
        List<FlowLog> flowLogs = flowLogDBService.getAllFlowLogsByResourceCrnOrName(resourceCrn);
        return flowProgressResponseConverter.convertList(flowLogs, resourceCrn);
    }

}
