package com.sequenceiq.cloudbreak.cloud.aws.util.poller.upgrade;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;

import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;

public class UpgradeStartWaitTask implements AttemptMaker<Boolean> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeStartWaitTask.class);

    private static final String RDS_UPGRADE_STATE = "upgrading";

    private final DescribeDbInstancesRequest describeDBInstancesRequest;

    private final AmazonRdsClient rdsClient;

    public UpgradeStartWaitTask(DescribeDbInstancesRequest request, AmazonRdsClient rdsClient) {
        this.describeDBInstancesRequest = request;
        this.rdsClient = rdsClient;
    }

    @Override
    public AttemptResult<Boolean> process() {
        DescribeDbInstancesResponse result = rdsClient.describeDBInstances(describeDBInstancesRequest);
        return isRdsInUpgradeState(result)
                ? AttemptResults.finishWith(Boolean.TRUE)
                : AttemptResults.justContinue();
    }

    private boolean isRdsInUpgradeState(DescribeDbInstancesResponse result) {
        Set<String> statuses = result.dbInstances().stream().map(DBInstance::dbInstanceStatus).collect(Collectors.toSet());
        LOGGER.debug("AWS RDS upgrade start check: status queried: {}", statuses);
        return result.dbInstances().stream()
                .map(DBInstance::dbInstanceStatus)
                .anyMatch(st -> RDS_UPGRADE_STATE.equals(st.toLowerCase(Locale.ROOT)));
    }

}
