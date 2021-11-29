package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.BackoffCancellablePollingStrategy.getBackoffCancellablePollingStrategy;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.waiters.Waiter;
import com.amazonaws.waiters.WaiterParameters;
import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsStackRequestHelper;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.StackCancellationCheck;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.service.Retry;

@Service
public class AwsRdsTerminateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsTerminateService.class);

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private AwsStackRequestHelper awsStackRequestHelper;

    @Inject
    @Qualifier("DefaultRetryService")
    private Retry retryService;

    /**
     * Terminates a database server (stack).
     *
     * @param ac                  authenticated cloud context
     * @param stack               database stack to delete
     * @param force               whether to continue even if stack termination fails in AWS
     * @param persistenceNotifier notifies Resources table of resource deletion
     * @param resources           list of resources tracked in DB
     * @return list of affected cloud resources (not yet implemented)
     * @throws AmazonServiceException  if the search for the stack fails
     * @throws ExecutionException      if stack deletion fails (and force is false)
     * @throws TimeoutException        if stack deletion times out
     * @throws InterruptedException    if the wait for stack deletion is interrupted
     * @throws CloudConnectorException if stack deletion fails due to a runtime exception
     */
    public List<CloudResourceStatus> terminate(AuthenticatedContext ac, DatabaseStack stack, boolean force,
            PersistenceNotifier persistenceNotifier, List<CloudResource> resources)
            throws Exception {
        String cFStackName = cfStackUtil.getCfStackName(ac);
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        try {
            initiateCFTemplateDeletion(
                    ac,
                    cFStackName,
                    credentialView,
                    regionName
            );
        } catch (AmazonServiceException e) {
            return getAmazonServiceException(force, cFStackName, e);
        } catch (Exception ex) {
            Exception runtimeException = getRuntimeException(force, cFStackName, ex);
            if (runtimeException != null) {
                throw runtimeException;
            }
        }

        CloudContext cloudContext = ac.getCloudContext();
        resources.forEach(r -> persistenceNotifier.notifyDeletion(r, cloudContext));

        // FIXME
        return List.of();
    }

    public Exception getRuntimeException(boolean force, String cFStackName, Exception e) {
        if (force) {
            LOGGER.warn("Stack deletion for '{}' failed, continuing because termination is forced", cFStackName, e);
            return null;
        } else {
            if (e instanceof RuntimeException) {
                return new CloudConnectorException("RDS termination failed " + e.getMessage(), e);
            } else {
                return e;
            }
        }
    }

    public List<CloudResourceStatus> getAmazonServiceException(boolean force, String cFStackName, AmazonServiceException e) {
        if (!e.getErrorMessage().contains(cFStackName + " does not exist") && !force) {
            throw e;
        }
        LOGGER.warn("Stack " + cFStackName + " does not exist, assuming that it has already been deleted");
        // FIXME
        return List.of();
    }

    private void initiateCFTemplateDeletion(
            AuthenticatedContext ac,
            String cFStackName,
            AwsCredentialView credentialView,
            String regionName
    ) {
        AmazonCloudFormationClient cfClient = awsClient.createCloudFormationClient(credentialView, regionName);
        boolean exists = retryService.testWith2SecDelayMax15Times(() -> cfStackUtil.isCfStackExists(cfClient, cFStackName));
        if (exists) {
            DeleteStackRequest deleteStackRequest = awsStackRequestHelper.createDeleteStackRequest(cFStackName);
            cfClient.deleteStack(deleteStackRequest);
            LOGGER.debug("CloudFormation stack deletion request sent with stack name: '{}' for stack: '{}'", cFStackName, ac.getCloudContext().getId());

            Waiter<DescribeStacksRequest> stackDeleteCompleteWaiter = cfClient.waiters().stackDeleteComplete();
            StackCancellationCheck stackCancellationCheck = new StackCancellationCheck(ac.getCloudContext().getId());
            DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(cFStackName);
            WaiterParameters<DescribeStacksRequest> describeStacksRequestWaiterParameters = new WaiterParameters<>(describeStacksRequest)
                    .withPollingStrategy(getBackoffCancellablePollingStrategy(stackCancellationCheck));
            stackDeleteCompleteWaiter.run(describeStacksRequestWaiterParameters);
        }
    }
}
