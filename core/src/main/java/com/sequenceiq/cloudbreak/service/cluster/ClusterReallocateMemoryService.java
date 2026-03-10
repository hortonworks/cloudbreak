package com.sequenceiq.cloudbreak.service.cluster;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.resetjvmparams.JvmConfigRecordV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.resetjvmparams.ResetJvmParamsDiffV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.resetjvmparams.ResetJvmParamsV4Response;
import com.sequenceiq.cloudbreak.cluster.model.resetjvmparams.JvmConfigRecord;
import com.sequenceiq.cloudbreak.cluster.model.resetjvmparams.ResetJvmParamsDiff;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class ClusterReallocateMemoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterReallocateMemoryService.class);

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private ReactorFlowManager flowManager;

    public ResetJvmParamsV4Response resetJvmParams(StackDto stack, boolean dryRun) {
        LOGGER.info("Triggering reset JVM params on stack ('{}'), dryRun={}", stack.getResourceCrn(), dryRun);
        MDCBuilder.buildMdcContext(stack);

        ResetJvmParamsDiff diff = clusterApiConnectors.getConnector(stack).reallocateMemoryDiff();
        ResetJvmParamsDiffV4Response diffResponse = toResetJvmParamsDiffV4Response(diff);

        if (dryRun) {
            LOGGER.info("Dry run requested, skipping flow trigger for stack ('{}')", stack.getResourceCrn());
            ResetJvmParamsV4Response response = new ResetJvmParamsV4Response(FlowIdentifier.notTriggered(),
                    "Dry run: reset JVM params was not triggered for stack: " + stack.getResourceCrn());
            response.setResetJvmParamsDiff(diffResponse);
            return response;
        }

        FlowIdentifier flowIdentifier = flowManager.triggerResetJvmParams(stack.getId());
        ResetJvmParamsV4Response response = new ResetJvmParamsV4Response(flowIdentifier,
                "Reset JVM params triggered for stack: " + stack.getResourceCrn());
        response.setResetJvmParamsDiff(diffResponse);
        return response;
    }

    private ResetJvmParamsDiffV4Response toResetJvmParamsDiffV4Response(ResetJvmParamsDiff diff) {
        if (diff == null) {
            return new ResetJvmParamsDiffV4Response();
        }
        return new ResetJvmParamsDiffV4Response(
                toJvmConfigRecordV4Responses(diff.getConfigsBefore()),
                toJvmConfigRecordV4Responses(diff.getConfigsAfter()));
    }

    private List<JvmConfigRecordV4Response> toJvmConfigRecordV4Responses(List<JvmConfigRecord> records) {
        if (records == null) {
            return List.of();
        }
        return records.stream()
                .map(r -> new JvmConfigRecordV4Response(
                        r.getName(),
                        r.getValue(),
                        r.getRoleConfigGroupName(),
                        r.getClusterName(),
                        r.getServiceName(),
                        r.getApplicability() != null ? r.getApplicability().name() : null))
                .collect(Collectors.toList());
    }
}
