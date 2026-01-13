package com.sequenceiq.environment.environment.flow.deletion.handler.freeipa;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.polling.SimpleStatusCheckerTask;
import com.sequenceiq.environment.environment.flow.creation.handler.freeipa.FreeIpaPollerObject;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

public class FreeIpaDeletionRetrievalTask extends SimpleStatusCheckerTask<FreeIpaPollerObject> {

    public static final int FREEIPA_RETRYING_INTERVAL = 5000;

    public static final int FREEIPA_RETRYING_COUNT = 900;

    public static final int FREEIPA_FAILURE_COUNT = 3;

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaDeletionRetrievalTask.class);

    private final FreeIpaService freeIpaService;

    private final FlowLogService flowLogService;

    public FreeIpaDeletionRetrievalTask(FreeIpaService freeIpaService, FlowLogService flowLogService) {
        this.freeIpaService = freeIpaService;
        this.flowLogService = flowLogService;
    }

    @Override
    public boolean checkStatus(FreeIpaPollerObject freeIpaPollerObject) {
        String environmentCrn = freeIpaPollerObject.getEnvironmentCrn();
        try {
            LOGGER.info("Checking the state of FreeIpa termination progress for environment: '{}'", environmentCrn);
            Optional<DescribeFreeIpaResponse> freeIpaResponseOptional = freeIpaService.describe(environmentCrn);
            if (freeIpaResponseOptional.isPresent()) {
                DescribeFreeIpaResponse freeIpaResponse = freeIpaResponseOptional.get();
                if (freeIpaResponse.getStatus() == Status.DELETE_FAILED) {
                    throw new FreeIpaOperationFailedException("FreeIpa deletion operation failed: " + freeIpaResponse.getStatusReason());
                }
                if (!freeIpaResponse.getStatus().isSuccessfullyDeleted()) {
                    if (!isFlowRunning(freeIpaPollerObject.getFlowIdentifier().getPollableId(), freeIpaPollerObject.getResourceId())) {
                        throw new FreeIpaOperationFailedException("FreeIpa deletion operation failed. Termination flow is finished but FreeIpa is not deleted");
                    } else {
                        return false;

                    }
                }
            } else {
                LOGGER.info("FreeIpa was not found.");
                return true;
            }
        } catch (Exception e) {
            throw new FreeIpaOperationFailedException("FreeIpa deletion operation failed. " + e.getMessage(), e);
        }
        return true;
    }

    @Override
    public void handleTimeout(FreeIpaPollerObject freeIpaPollerObject) {
        try {
            String envCrn = freeIpaPollerObject.getEnvironmentCrn();
            Optional<DescribeFreeIpaResponse> freeIpa = freeIpaService.describe(envCrn);
            if (freeIpa.isEmpty()) {
                throw new FreeIpaOperationFailedException("FreeIpa was not found for environment: " + envCrn);
            }
            throw new FreeIpaOperationFailedException(String.format("Polling operation timed out, FreeIpa deletion failed. FreeIpa status: '%s' "
                    + "statusReason: '%s'", freeIpa.get().getStatus(), freeIpa.get().getStatusReason()));
        } catch (Exception e) {
            throw new FreeIpaOperationFailedException("Polling operation timed out, FreeIpa deletion failed. Also failed to get FreeIpa status: "
                    + e.getMessage(), e);
        }
    }

    @Override
    public String successMessage(FreeIpaPollerObject freeIpaPollerObject) {
        return "FreeIpa deletion successfully finished.";
    }

    @Override
    public boolean exitPolling(FreeIpaPollerObject freeIpaPollerObject) {
        return false;
    }

    private boolean isFlowRunning(String flowId, Long resourceId) {
        if (flowId == null) {
            return false;
        }

        List<FlowLog> flowLogs = flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(resourceId);
        return flowLogs
                .stream()
                .anyMatch(flowLog -> flowLog.getFlowId().equals(flowId));
    }

}
