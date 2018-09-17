package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.v3.DatalakeClusterV3Action;

public class DatalakeCluster extends AbstractCloudbreakEntity<StackV2Request, StackResponse, DatalakeCluster> {

    private static final String DATALAKE_CLUSTER_ID = "DATALAKE_CLUSTER";

    public DatalakeCluster() {
        this(DATALAKE_CLUSTER_ID);
    }

    private DatalakeCluster(String id) {
        super(id);
    }

    public DatalakeCluster(TestContext testContext) {
        super(new StackV2Request(), testContext);
    }

    public static DatalakeCluster isCreatedWithName(String name) {
        var datalake = new DatalakeCluster();
        datalake.setCreationStrategy((testContext, entity) -> DatalakeClusterV3Action.get(testContext, entity, name));
        return datalake;
    }

    public static Function<IntegrationTestContext, DatalakeCluster> getTestContextDatalakeCluster(String key) {
        return testContext -> testContext.getContextParam(key, DatalakeCluster.class);
    }

    public static Function<IntegrationTestContext, DatalakeCluster> getTestContextDatalakeCluster() {
        return getTestContextDatalakeCluster(DATALAKE_CLUSTER_ID);
    }

    static Function<IntegrationTestContext, DatalakeCluster> getNewDatalakeCluster() {
        return testContext -> new DatalakeCluster();
    }
}
