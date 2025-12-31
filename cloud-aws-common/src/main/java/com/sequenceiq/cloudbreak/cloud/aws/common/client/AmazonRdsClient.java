package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsPageCollector;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.Certificate;
import software.amazon.awssdk.services.rds.model.CreateDbParameterGroupRequest;
import software.amazon.awssdk.services.rds.model.CreateDbParameterGroupResponse;
import software.amazon.awssdk.services.rds.model.DbParameterGroupNotFoundException;
import software.amazon.awssdk.services.rds.model.DeleteDbParameterGroupRequest;
import software.amazon.awssdk.services.rds.model.DescribeCertificatesRequest;
import software.amazon.awssdk.services.rds.model.DescribeCertificatesResponse;
import software.amazon.awssdk.services.rds.model.DescribeDbEngineVersionsRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbEngineVersionsResponse;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.rds.model.DescribeDbParameterGroupsRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbParameterGroupsResponse;
import software.amazon.awssdk.services.rds.model.DescribeOrderableDbInstanceOptionsRequest;
import software.amazon.awssdk.services.rds.model.InvalidDbParameterGroupStateException;
import software.amazon.awssdk.services.rds.model.ModifyDbInstanceRequest;
import software.amazon.awssdk.services.rds.model.ModifyDbInstanceResponse;
import software.amazon.awssdk.services.rds.model.ModifyDbParameterGroupRequest;
import software.amazon.awssdk.services.rds.model.ModifyDbParameterGroupResponse;
import software.amazon.awssdk.services.rds.model.Parameter;
import software.amazon.awssdk.services.rds.model.RebootDbInstanceRequest;
import software.amazon.awssdk.services.rds.model.RebootDbInstanceResponse;
import software.amazon.awssdk.services.rds.model.StartDbInstanceRequest;
import software.amazon.awssdk.services.rds.model.StartDbInstanceResponse;
import software.amazon.awssdk.services.rds.model.StopDbInstanceRequest;
import software.amazon.awssdk.services.rds.model.StopDbInstanceResponse;
import software.amazon.awssdk.services.rds.paginators.DescribeOrderableDBInstanceOptionsIterable;
import software.amazon.awssdk.services.rds.waiters.RdsWaiter;

public class AmazonRdsClient extends AmazonClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmazonRdsClient.class);

    private final RdsClient client;

    private final AwsPageCollector awsPageCollector;

    public AmazonRdsClient(RdsClient client, AwsPageCollector awsPageCollector) {
        this.client = client;
        this.awsPageCollector = awsPageCollector;
    }

    public ModifyDbInstanceResponse modifyDBInstance(ModifyDbInstanceRequest modifyDBInstanceRequest) {
        return client.modifyDBInstance(modifyDBInstanceRequest);
    }

    public StartDbInstanceResponse startDBInstance(StartDbInstanceRequest startDBInstanceRequest) {
        return client.startDBInstance(startDBInstanceRequest);
    }

    // FIXME return actual waiter instead
    public RdsWaiter waiters() {
        return client.waiter();
    }

    public DescribeDbInstancesResponse describeDBInstances(DescribeDbInstancesRequest describeDBInstancesRequest) {
        return client.describeDBInstances(describeDBInstancesRequest);
    }

    public DescribeOrderableDBInstanceOptionsIterable describeOrderableDbInstanceOptionsResponse(
            DescribeOrderableDbInstanceOptionsRequest describeOrderableDbInstanceOptionsRequest) {
        return client.describeOrderableDBInstanceOptionsPaginator(describeOrderableDbInstanceOptionsRequest);
    }

    public boolean isDbParameterGroupPresent(String dbParameterGroupName) {
        DescribeDbParameterGroupsRequest describeDBParameterGroupsRequest = DescribeDbParameterGroupsRequest.builder()
                .dbParameterGroupName(dbParameterGroupName).build();
        try {
            DescribeDbParameterGroupsResponse result = client.describeDBParameterGroups(describeDBParameterGroupsRequest);
            LOGGER.debug("DB parameter group was found: {}, result: {}", dbParameterGroupName, result);
            return true;
        } catch (DbParameterGroupNotFoundException e) {
            LOGGER.debug("DB parameter group was not found: {}", dbParameterGroupName);
            return false;
        }
    }

    public CreateDbParameterGroupResponse createParameterGroup(String dbParameterGroupFamily, String dbParameterGroupName, String dbParameterGroupDescription) {
        CreateDbParameterGroupRequest createDBParameterGroupRequest = CreateDbParameterGroupRequest.builder()
                .dbParameterGroupFamily(dbParameterGroupFamily)
                .dbParameterGroupName(dbParameterGroupName)
                .description(dbParameterGroupDescription)
                .build();
        return client.createDBParameterGroup(createDBParameterGroupRequest);
    }

    public ModifyDbParameterGroupResponse changeParameterInGroup(String dbParameterGroupName, List<Parameter> parameters) {
        ModifyDbParameterGroupRequest modifyDBParameterGroupRequest = ModifyDbParameterGroupRequest.builder()
                .dbParameterGroupName(dbParameterGroupName)
                .parameters(parameters)
                .build();

        return client.modifyDBParameterGroup(modifyDBParameterGroupRequest);
    }

    public void deleteParameterGroup(String dbParameterGroupName) {
        try {
            if (isDbParameterGroupPresent(dbParameterGroupName)) {
                client.deleteDBParameterGroup(
                        DeleteDbParameterGroupRequest.builder()
                        .dbParameterGroupName(dbParameterGroupName)
                        .build()
                );
            }
        } catch (DbParameterGroupNotFoundException e) {
            LOGGER.debug("ParameterGroup with {} name does not exist", dbParameterGroupName, e);
        } catch (InvalidDbParameterGroupStateException e) {
            String msg = String.format("The DB parameter group [%s] is in use or is in an invalid state. If you are attempting to delete the parameter group," +
                    " you can't delete it when the parameter group is in this state.", dbParameterGroupName);
            LOGGER.error(msg, e);
            throw new CloudConnectorException(msg, e);
        }
    }

    public StopDbInstanceResponse stopDBInstance(StopDbInstanceRequest stopDBInstanceRequest) {
        return client.stopDBInstance(stopDBInstanceRequest);
    }

    public RebootDbInstanceResponse rebootDBInstance(RebootDbInstanceRequest rebootDbInstanceRequest) {
        return client.rebootDBInstance(rebootDbInstanceRequest);
    }

    public List<Certificate> describeCertificates(DescribeCertificatesRequest request) {
        return awsPageCollector.collectPages(this::describeCertificatesInternal,
                request,
                DescribeCertificatesResponse::certificates,
                DescribeCertificatesResponse::marker,
                (req, token) -> req.toBuilder().marker(token).build());
    }

    public DescribeDbEngineVersionsResponse describeDBEngineVersions(DescribeDbEngineVersionsRequest describeDBEngineVersionsRequest) {
        return client.describeDBEngineVersions(describeDBEngineVersionsRequest);
    }

    private DescribeCertificatesResponse describeCertificatesInternal(DescribeCertificatesRequest request) {
        return client.describeCertificates(request);
    }

}
