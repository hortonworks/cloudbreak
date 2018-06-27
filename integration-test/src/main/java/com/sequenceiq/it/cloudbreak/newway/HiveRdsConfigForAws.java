package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.cloudbreak.api.model.rds.RdsType.HIVE;
import static com.sequenceiq.it.cloudbreak.newway.cloud.AwsCloudProvider.AWS;

public class HiveRdsConfigForAws extends RdsConfig {

    private static final String HIVE_RDS_CONFIG = "AWS_HIVE_RDS_CONFIG";

    private HiveRdsConfigForAws() {
        super(HIVE_RDS_CONFIG);
    }

    public static Action<RdsConfig> post() {
        return post(HIVE_RDS_CONFIG);
    }

    public static Action<RdsConfig> get() {
        return get(HIVE_RDS_CONFIG);
    }

    public static Action<RdsConfig> delete() {
        return delete(HIVE_RDS_CONFIG);
    }

    public static Action<RdsConfig> testConnect() {
        return testConnect(HIVE_RDS_CONFIG);
    }

    public static RdsConfig isCreatedWithParameters(TestParameter testParameter) {
        RdsConfig hive = new HiveRdsConfigForAws();
        hive.setRequest(RDSConfigRequestDataCollector.createRdsRequestWithProperties(testParameter, HIVE, AWS));
        hive.setCreationStrategy(RdsConfigAction::createInGiven);
        return hive;
    }
}
