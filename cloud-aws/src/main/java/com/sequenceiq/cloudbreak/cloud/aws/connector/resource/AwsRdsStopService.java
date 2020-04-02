package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.StopDBInstanceRequest;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.AwsBackoffSyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Service
public class AwsRdsStopService {

    private static final String SUCCESS_STATUS = "stopped";

    @Inject
    private AwsClient awsClient;

    @Inject
    private AwsPollTaskFactory awsPollTaskFactory;

    @Inject
    private AwsBackoffSyncPollingScheduler<Boolean> awsBackoffSyncPollingScheduler;

    public void stop(AuthenticatedContext ac, String dbInstanceIdentifier) throws ExecutionException, TimeoutException, InterruptedException {
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AmazonRDS rdsClient = awsClient.createRdsClient(credentialView, regionName);

        StopDBInstanceRequest stopDBInstanceRequest = new StopDBInstanceRequest();
        stopDBInstanceRequest.setDBInstanceIdentifier(dbInstanceIdentifier);

        try {
            rdsClient.stopDBInstance(stopDBInstanceRequest);
        } catch (RuntimeException ex) {
            throw new CloudConnectorException(ex.getMessage(), ex);
        }

        PollTask<Boolean> task = awsPollTaskFactory.newRdbStatusCheckerTask(ac, dbInstanceIdentifier, SUCCESS_STATUS, rdsClient);
        try {
            awsBackoffSyncPollingScheduler.schedule(task);
        } catch (RuntimeException e) {
            throw new CloudConnectorException(e.getMessage(), e);
        }
    }
}
