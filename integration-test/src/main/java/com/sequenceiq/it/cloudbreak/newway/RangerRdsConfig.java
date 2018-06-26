package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.cloudbreak.api.model.rds.RdsType.RANGER;

public class RangerRdsConfig extends RdsConfig {

    private static final String RANGER_RDS_CONFIG = "RANGER_RDS_CONFIG";

    private RangerRdsConfig() {
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
        RdsConfig ranger = new RangerRdsConfig();
        ranger.setRequest(RDSConfigRequestDataCollector.createRdsRequestWithProperties(testParameter, RANGER));
        ranger.setCreationStrategy(RdsConfigAction::createInGiven);
        return ranger;
    }
}
