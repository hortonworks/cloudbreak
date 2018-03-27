package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RdsTestResult;
import com.sequenceiq.it.IntegrationTestContext;

public class RdsTestEntity extends AbstractCloudbreakEntity<RDSConfigRequest, RdsTestResult> {
    private static final String RDS_TEST = "RDS_TEST";

    RdsTestEntity(String newId) {
        super(newId);
        setRequest(new RDSConfigRequest());
    }

    RdsTestEntity() {
        this(RDS_TEST);
    }

    private static Function<IntegrationTestContext, RdsTest> getTestContext(String key) {
        return (testContext) -> testContext.getContextParam(key, RdsTest.class);
    }

    public RdsTestEntity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public RdsTestEntity withConnectionPassword(String password) {
        getRequest().setConnectionPassword(password);
        return this;
    }

    public RdsTestEntity withConnectionUserName(String username) {
        getRequest().setConnectionUserName(username);
        return this;
    }

    public RdsTestEntity withConnectionURL(String connectionURL) {
        getRequest().setConnectionURL(connectionURL);
        return this;
    }

    public RdsTestEntity withType(String type) {
        getRequest().setType(type);
        return this;
    }
}