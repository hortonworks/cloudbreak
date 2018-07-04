package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.cloudbreak.api.model.rds.RdsType.RANGER;
import static com.sequenceiq.it.cloudbreak.newway.cloud.GcpCloudProvider.GCP;

public class RangerRdsConfigForGcp extends RdsConfig {

    private static final String RANGER_RDS_CONFIG = "GCP_RANGER_DB_CONFIG";

    private RangerRdsConfigForGcp() {
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
        RdsConfig ranger = new RangerRdsConfigForGcp();
        ranger.setRequest(RDSConfigRequestDataCollector.createRdsRequestWithProperties(testParameter, RANGER, GCP));
        ranger.setCreationStrategy(RdsConfigAction::createInGiven);
        return ranger;
    }
}
