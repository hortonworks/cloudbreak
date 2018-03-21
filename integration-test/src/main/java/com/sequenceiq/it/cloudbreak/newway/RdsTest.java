package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.it.IntegrationTestContext;


public class RdsTest extends RdsTestEntity {
    private static final String RDSTEST = "RDSTEST";

    private final RDSConfigRequest rdsConfigRequest = new RDSConfigRequest();

    private RdsTest() {
        super(RDSTEST);
    }

    private static Function<IntegrationTestContext, RdsTest> getTestContext(String key) {
        return (testContext) -> testContext.getContextParam(key, RdsTest.class);
    }

    static Function<IntegrationTestContext, RdsTest> getNew() {
        return (testContext)->new RdsTest();
    }

    public static RdsTest request() {
        return new RdsTest();
    }

    public RDSConfigRequest getRequest() {
        return rdsConfigRequest;
    }

    public static Action<RdsTest> testConnect(String key) {
        return new Action<>(getTestContext(key), RdsConfigAction::testConnect);
    }

    public static Action<RdsTest> testConnect() {
        return testConnect(RDSTEST);
    }

    public static Assertion<RdsTest> assertThis(BiConsumer<RdsTest, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }
}