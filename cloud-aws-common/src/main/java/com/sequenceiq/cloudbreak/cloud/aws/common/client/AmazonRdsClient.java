package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.Certificate;
import com.amazonaws.services.rds.model.CreateDBParameterGroupRequest;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBParameterGroup;
import com.amazonaws.services.rds.model.DBParameterGroupNotFoundException;
import com.amazonaws.services.rds.model.DescribeCertificatesRequest;
import com.amazonaws.services.rds.model.DescribeCertificatesResult;
import com.amazonaws.services.rds.model.DescribeDBEngineVersionsRequest;
import com.amazonaws.services.rds.model.DescribeDBEngineVersionsResult;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.DescribeDBParameterGroupsRequest;
import com.amazonaws.services.rds.model.DescribeDBParameterGroupsResult;
import com.amazonaws.services.rds.model.ModifyDBInstanceRequest;
import com.amazonaws.services.rds.model.ModifyDBParameterGroupRequest;
import com.amazonaws.services.rds.model.ModifyDBParameterGroupResult;
import com.amazonaws.services.rds.model.Parameter;
import com.amazonaws.services.rds.model.StartDBInstanceRequest;
import com.amazonaws.services.rds.model.StopDBInstanceRequest;
import com.amazonaws.services.rds.waiters.AmazonRDSWaiters;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsPageCollector;

public class AmazonRdsClient extends AmazonClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmazonRdsClient.class);

    private final AmazonRDS client;

    private final AwsPageCollector awsPageCollector;

    public AmazonRdsClient(AmazonRDS client, AwsPageCollector awsPageCollector) {
        this.client = client;
        this.awsPageCollector = awsPageCollector;
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

    public boolean isDbParameterGroupPresent(String dbParameterGroupName) {
        DescribeDBParameterGroupsRequest describeDBParameterGroupsRequest = new DescribeDBParameterGroupsRequest()
                .withDBParameterGroupName(dbParameterGroupName);
        try {
            DescribeDBParameterGroupsResult result = client.describeDBParameterGroups(describeDBParameterGroupsRequest);
            LOGGER.debug("DB parameter group was found: {}, result: {}", dbParameterGroupName, result);
            return true;
        } catch (DBParameterGroupNotFoundException e) {
            LOGGER.debug("DB parameter group was not found: {}", dbParameterGroupName);
            return false;
        }
    }

    public DBParameterGroup createParameterGroup(String dbParameterGroupFamily, String dbParameterGroupName, String dbParameterGroupDescription) {
        CreateDBParameterGroupRequest createDBParameterGroupRequest = new CreateDBParameterGroupRequest()
                .withDBParameterGroupFamily(dbParameterGroupFamily)
                .withDBParameterGroupName(dbParameterGroupName)
                .withDescription(dbParameterGroupDescription);
        return client.createDBParameterGroup(createDBParameterGroupRequest);
    }

    public ModifyDBParameterGroupResult changeParameterInGroup(String dbParameterGroupName, List<Parameter> parameters) {
        ModifyDBParameterGroupRequest modifyDBParameterGroupRequest = new ModifyDBParameterGroupRequest()
                .withDBParameterGroupName(dbParameterGroupName)
                .withParameters(parameters);

        return client.modifyDBParameterGroup(modifyDBParameterGroupRequest);
    }

    public DBInstance stopDBInstance(StopDBInstanceRequest stopDBInstanceRequest) {
        return client.stopDBInstance(stopDBInstanceRequest);
    }

    public List<Certificate> describeCertificates(DescribeCertificatesRequest request) {
        return awsPageCollector.collectPages(this::describeCertificatesInternal,
                request,
                DescribeCertificatesResult::getCertificates,
                DescribeCertificatesResult::getMarker,
                DescribeCertificatesRequest::setMarker);
    }

    public DescribeDBEngineVersionsResult describeDBEngineVersions(DescribeDBEngineVersionsRequest describeDBEngineVersionsRequest) {
        return client.describeDBEngineVersions(describeDBEngineVersionsRequest);
    }

    private DescribeCertificatesResult describeCertificatesInternal(DescribeCertificatesRequest request) {
        return client.describeCertificates(request);
    }

}
