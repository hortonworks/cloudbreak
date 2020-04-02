package com.sequenceiq.cloudbreak.cloud.aws.task;

import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AwsRdbStatusCheckerTaskTest {

    private static final String DB_INSTANCE_IDENTIFIER = "dbInstanceIdentifier";

    private static final String SUCCESS_STATUS = "successStatus";

    private static final String NOT_SUCCESS_STATUS = "notSuccessStatus";

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private AmazonRDSClient client;

    @Mock
    private DescribeDBInstancesResult describeDBInstancesResult;

    @Mock
    private DBInstance dbInstance;

    private AwsRdbStatusCheckerTask victim;

    @Test
    public void doCallShouldReturnTrueWhenInstanceInSuccessStatus() {
        initTest();

        when(dbInstance.getDBInstanceStatus()).thenReturn(SUCCESS_STATUS);

        Boolean result = victim.doCall();

        assertThat(result).isTrue();
    }

    @Test
    public void doCallShouldReturnFalseWhenInstanceNotInSuccessStatus() {
        initTest();

        when(dbInstance.getDBInstanceStatus()).thenReturn(NOT_SUCCESS_STATUS);

        Boolean result = victim.doCall();

        assertThat(result).isFalse();
    }

    private void initTest() {
        victim = new AwsRdbStatusCheckerTask(authenticatedContext, DB_INSTANCE_IDENTIFIER, SUCCESS_STATUS, client);

        when(client.describeDBInstances(any(DescribeDBInstancesRequest.class))).thenReturn(describeDBInstancesResult);
        when(describeDBInstancesResult.getDBInstances()).thenReturn(singletonList(dbInstance));
    }
}