package com.sequenceiq.cloudbreak.cloud.aws.task;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryResourceStateStore;

@Scope("prototype")
@Component(AwsCreateNetworkStatusCheckerTask.NAME)
public class AwsCreateNetworkStatusCheckerTask extends AbstractAwsStackStatusCheckerTask {

    public static final String NAME = "awsCreateNetworkStatusCheckerTask";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCreateNetworkStatusCheckerTask.class);

    private final NetworkCreationRequest networkCreationRequest;

    public AwsCreateNetworkStatusCheckerTask(AmazonCloudFormationClient cfClient, StackStatus successStatus, StackStatus errorStatus,
            List<StackStatus> stackErrorStatuses, NetworkCreationRequest networkCreationRequest) {
        super(null, cfClient, successStatus, errorStatus, stackErrorStatuses, networkCreationRequest.getStackName(), true);
        this.networkCreationRequest = networkCreationRequest;
    }

    @Override
    protected boolean doCheck(Stack cfStack, List<StackEvent> stackEvents) {
        return isSuccess(cfStack, stackEvents);
    }

    @Override
    protected boolean handleError(AmazonServiceException e) {
        throw new CloudConnectorException(getErrorMessage(e.getErrorCode(), e.getErrorMessage()));
    }

    @Override
    public boolean cancelled() {
        PollGroup environmentPollGroup = InMemoryResourceStateStore.getResource("environment", networkCreationRequest.getEnvId());
        if (environmentPollGroup == null || environmentPollGroup.isCancelled()) {
            LOGGER.info("Cancelling the polling of environment's '{}' Network creation, because a delete operation has already been "
                    + "started on the environment", networkCreationRequest.getEnvName());
            return true;
        }
        return false;
    }
}
