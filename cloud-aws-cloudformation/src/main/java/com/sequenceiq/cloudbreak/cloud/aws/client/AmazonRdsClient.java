package com.sequenceiq.cloudbreak.cloud.aws.client;

import java.util.List;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.Certificate;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeCertificatesRequest;
import com.amazonaws.services.rds.model.DescribeCertificatesResult;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.ModifyDBInstanceRequest;
import com.amazonaws.services.rds.model.StartDBInstanceRequest;
import com.amazonaws.services.rds.model.StopDBInstanceRequest;
import com.amazonaws.services.rds.waiters.AmazonRDSWaiters;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsPageCollector;

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

    public List<Certificate> describeCertificates(DescribeCertificatesRequest request) {
        return AwsPageCollector.collectPages(this::describeCertificatesInternal,
                request,
                DescribeCertificatesResult::getCertificates,
                DescribeCertificatesResult::getMarker,
                DescribeCertificatesRequest::setMarker);
    }

    private DescribeCertificatesResult describeCertificatesInternal(DescribeCertificatesRequest request) {
        return client.describeCertificates(request);
    }

}
