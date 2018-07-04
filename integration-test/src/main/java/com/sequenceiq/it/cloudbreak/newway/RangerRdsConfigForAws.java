package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.cloudbreak.api.model.rds.RdsType.RANGER;
import static com.sequenceiq.it.cloudbreak.newway.cloud.AwsCloudProvider.AWS;

public class RangerRdsConfigForAws extends RdsConfig {

    private static final String RANGER_RDS_CONFIG = "AWS_RANGER_DB_CONFIG";

    private RangerRdsConfigForAws() {
        super(RANGER_RDS_CONFIG);
    }

    public static Action<RdsConfig> post() {
        return post(RANGER_RDS_CONFIG);
    }

    public static Action<RdsConfig> get() {
        return get(RANGER_RDS_CONFIG);
    }

    public static Action<RdsConfig> delete() {
        return delete(RANGER_RDS_CONFIG);
    }

    public static Action<RdsConfig> testConnect() {
        return testConnect(RANGER_RDS_CONFIG);
    }

    public static RdsConfig isCreatedWithParameters(TestParameter testParameter) {
        RdsConfig ranger = new RangerRdsConfigForAws();
        ranger.setRequest(RDSConfigRequestDataCollector.createRdsRequestWithProperties(testParameter, RANGER, AWS));
        ranger.setCreationStrategy(RdsConfigAction::createInGiven);
        return ranger;
    }
}
