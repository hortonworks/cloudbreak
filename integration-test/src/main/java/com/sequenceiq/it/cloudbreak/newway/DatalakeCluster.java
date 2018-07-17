package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.it.IntegrationTestContext;

public class DatalakeCluster extends AbstractCloudbreakEntity<StackV2Request, StackResponse> {

    private static final String DATALAKE_CLUSTER_ID = "DATALAKE_CLUSTER";

    public DatalakeCluster() {
        this(DATALAKE_CLUSTER_ID);
    }

    private DatalakeCluster(String id) {
        super(id);
    }

    public static DatalakeCluster isCreatedWithName(String name) {
        var datalake = new DatalakeCluster();
        datalake.setCreationStrategy((testContext, entity) -> DatalakeClusterAction.get(testContext, entity, name));
        return datalake;
    }

    public static Function<IntegrationTestContext, DatalakeCluster> getTestContextDatalakeCluster(String key) {
        return testContext -> testContext.getContextParam(key, DatalakeCluster.class);
    }

    static Function<IntegrationTestContext, DatalakeCluster> getTestContextDatalakeCluster() {
        return getTestContextDatalakeCluster(DATALAKE_CLUSTER_ID);
    }

    static Function<IntegrationTestContext, DatalakeCluster> getNewDatalakeCluster() {
        return testContext -> new DatalakeCluster();
    }
}
