package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsStackRequestHelper;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.AwsBackoffSyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.amazonaws.services.cloudformation.model.StackStatus.DELETE_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsResourceConstants.ERROR_STATUSES;

@Service
public class AwsRdsTerminateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsTerminateService.class);

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private AwsClient awsClient;

    @Inject
    private AwsBackoffSyncPollingScheduler<Boolean> awsBackoffSyncPollingScheduler;

    @Inject
    private AwsPollTaskFactory awsPollTaskFactory;

    @Inject
    private AwsStackRequestHelper awsStackRequestHelper;

    /**
     * Terminates a database server (stack).
     *
     * @param  ac                      authenticated cloud context
     * @param  stack                   database stack to delete
     * @param  force                   whether to continue even if stack termination fails in AWS
     * @return                         list of affected cloud resources (not yet implemented)
     * @throws AmazonServiceException  if the search for the stack fails
     * @throws ExecutionException      if stack deletion fails (and force is false)
     * @throws TimeoutException        if stack deletion times out
     * @throws InterruptedException    if the wait for stack deletion is interrupted
     * @throws CloudConnectorException if stack deletion fails due to a runtime exception
     */
    public List<CloudResourceStatus> terminate(AuthenticatedContext ac, DatabaseStack stack, boolean force)
            throws ExecutionException, TimeoutException, InterruptedException {
        // CloudResource stackResource = cfStackUtil.getCloudFormationStackResource(resources); get name from this?
        String cFStackName = cfStackUtil.getCfStackName(ac);
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AmazonCloudFormationRetryClient cfRetryClient = awsClient.createCloudFormationRetryClient(credentialView, regionName);
        try {
            cfRetryClient.describeStacks(new DescribeStacksRequest().withStackName(cFStackName));
        } catch (AmazonServiceException e) {
            if (!e.getErrorMessage().contains(cFStackName + " does not exist")) {
                throw e;
            }
            LOGGER.warn("Stack " + cFStackName + " does not exist, assuming that it has already been deleted");
            // FIXME
            return List.of();
        }

        cfRetryClient.deleteStack(awsStackRequestHelper.createDeleteStackRequest(cFStackName));
        LOGGER.debug("CloudFormation stack deletion request sent with stack name: '{}' for stack: '{}'", cFStackName, ac.getCloudContext().getId());

        AmazonCloudFormationClient cfClient = awsClient.createCloudFormationClient(credentialView, regionName);
        PollTask<Boolean> task = awsPollTaskFactory.newAwsTerminateStackStatusCheckerTask(ac, cfClient, DELETE_COMPLETE, DELETE_FAILED, ERROR_STATUSES,
                cFStackName);
        try {
            awsBackoffSyncPollingScheduler.schedule(task);
        } catch (ExecutionException | TimeoutException | RuntimeException e) {
            if (force) {
                LOGGER.warn("Stack deletion for '{}' failed, continuing because termination is forced", cFStackName, e);
            } else {
                if (e instanceof RuntimeException) {
                    throw new CloudConnectorException(e.getMessage(), e);
                } else {
                    throw e;
                }
            }
        }

        // FIXME
        return List.of();
    }

}
