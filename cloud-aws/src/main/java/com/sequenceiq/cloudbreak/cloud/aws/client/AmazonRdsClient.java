package com.sequenceiq.cloudbreak.cloud.aws.client;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.ModifyDBInstanceRequest;
import com.amazonaws.services.rds.model.StartDBInstanceRequest;
import com.amazonaws.services.rds.model.StopDBInstanceRequest;
import com.amazonaws.services.rds.waiters.AmazonRDSWaiters;

public class AmazonRdsClient extends AmazonClient {

    private final AmazonRDS client;

    public AmazonRdsClient(AmazonRDS client) {
        this.client = client;
    }

    public DBInstance modifyDBInstance(ModifyDBInstanceRequest modifyDBInstanceRequest) {
        return client.modifyDBInstance(modifyDBInstanceRequest);
    }

    public DescribeDBInstancesResult describeDbInstances(DescribeDBInstancesRequest describeDBInstancesRequest) {
        return client.describeDBInstances(describeDBInstancesRequest);
    }

    public DBInstance startDBInstance(StartDBInstanceRequest startDBInstanceRequest) {
        return client.startDBInstance(startDBInstanceRequest);
    }

    // FIXME return actual waiter instead
    public AmazonRDSWaiters waiters() {
        return client.waiters();
    }

    public DescribeDBInstancesResult describeDBInstances(DescribeDBInstancesRequest describeDBInstancesRequest) {
        return client.describeDBInstances(describeDBInstancesRequest);
    }

    public DBInstance stopDBInstance(StopDBInstanceRequest stopDBInstanceRequest) {
        return client.stopDBInstance(stopDBInstanceRequest);
    }
}
