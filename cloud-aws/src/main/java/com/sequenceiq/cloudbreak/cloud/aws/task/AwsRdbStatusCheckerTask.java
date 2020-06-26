package com.sequenceiq.cloudbreak.cloud.aws.task;

import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component(AwsRdbStatusCheckerTask.NAME)
@Scope("prototype")
public class AwsRdbStatusCheckerTask extends PollBooleanStateTask {

    public static final String NAME = "AwsRdbStatusCheckerTask";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdbStatusCheckerTask.class);

    private final String dbInstanceIdentifier;

    private final String successStatus;

    private final AmazonRDSClient client;

    public AwsRdbStatusCheckerTask(AuthenticatedContext authenticatedContext, String dbInstanceIdentifier, String successStatus, AmazonRDSClient client) {
        super(authenticatedContext, false);
        this.dbInstanceIdentifier = dbInstanceIdentifier;
        this.successStatus = successStatus;
        this.client = client;
    }

    @Override
    protected Boolean doCall() {
        LOGGER.debug("Checking '{}' RDB instance status is: '{}'", dbInstanceIdentifier, successStatus);

        DescribeDBInstancesResult describeDBInstancesResult = client.describeDBInstances(new DescribeDBInstancesRequest()
                .withDBInstanceIdentifier(dbInstanceIdentifier));
        boolean completed = describeDBInstancesResult
                .getDBInstances()
                .stream()
                .allMatch(i -> i.getDBInstanceStatus().equalsIgnoreCase(successStatus));
        if (!completed) {
            LOGGER.info("RDB instance status of '{}' is: '{}'", dbInstanceIdentifier, describeDBInstancesResult
                    .getDBInstances()
                    .stream()
                    .map(i -> i.getDBInstanceStatus())
                    .collect(Collectors.joining(", ")));
        }
        return completed;
    }
}
