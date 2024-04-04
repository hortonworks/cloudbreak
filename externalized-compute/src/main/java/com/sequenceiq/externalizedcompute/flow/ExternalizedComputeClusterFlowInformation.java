package com.sequenceiq.externalizedcompute.flow;

import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteFlowEvent.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_INITIATED_EVENT;

import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeClusterStatusEnum;
import com.sequenceiq.externalizedcompute.flow.create.ExternalizedComputeClusterCreateFlowConfig;
import com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteFlowConfig;
import com.sequenceiq.externalizedcompute.service.ExternalizedComputeClusterStatusService;
import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.flow.domain.FlowLog;

@Component
public class ExternalizedComputeClusterFlowInformation implements ApplicationFlowInformation {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeClusterFlowInformation.class);

    private static final List<String> ALLOWED_PARALLEL_FLOWS = Collections.singletonList(EXTERNALIZED_COMPUTE_CLUSTER_DELETE_INITIATED_EVENT.event());

    @Inject
    private ExternalizedComputeClusterStatusService externalizedComputeClusterStatusService;

    @Override
    public List<String> getAllowedParallelFlows() {
        return ALLOWED_PARALLEL_FLOWS;
    }

    @Override
    public List<Class<? extends FlowConfiguration<?>>> getTerminationFlow() {
        return Collections.singletonList(ExternalizedComputeClusterDeleteFlowConfig.class);
    }

    @Override
    public void handleFlowFail(FlowLog flowLog) {
        if (ExternalizedComputeClusterCreateFlowConfig.class.equals(flowLog.getFlowType())) {
            externalizedComputeClusterStatusService.setStatus(flowLog.getResourceId(), ExternalizedComputeClusterStatusEnum.CREATE_FAILED, "Flow failed");
        } else if (ExternalizedComputeClusterDeleteFlowConfig.class.equals(flowLog.getFlowType())) {
            externalizedComputeClusterStatusService.setStatus(flowLog.getResourceId(), ExternalizedComputeClusterStatusEnum.DELETE_FAILED, "Flow failed");
        } else {
            externalizedComputeClusterStatusService.setStatus(flowLog.getResourceId(), ExternalizedComputeClusterStatusEnum.UNKNOWN, "Flow failed");
        }
    }
}
