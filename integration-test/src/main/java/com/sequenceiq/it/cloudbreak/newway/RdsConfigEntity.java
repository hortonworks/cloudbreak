package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigResponse;
import com.sequenceiq.cloudbreak.api.model.rds.RdsTestResult;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

import javax.ws.rs.WebApplicationException;

public class RdsConfigEntity extends AbstractCloudbreakEntity<RDSConfigRequest, RDSConfigResponse, RdsConfigEntity> {

    public static final String RDS_CONFIG = "RDS_CONFIG";

    private RdsTestResult response;

    RdsConfigEntity(String newId) {
        super(newId);
        setRequest(new RDSConfigRequest());
    }

    RdsConfigEntity() {
        this(RDS_CONFIG);
    }

    public RdsConfigEntity(TestContext testContext) {
        super(new RDSConfigRequest(), testContext);
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        try {
            cloudbreakClient.getCloudbreakClient().rdsConfigV3Endpoint().deleteInWorkspace(cloudbreakClient.getWorkspaceId(), getName());
        } catch (WebApplicationException ignore) {
            LOGGER.info("Something happend.");
        }
    }

    public RdsConfigEntity valid() {
        return withName(getNameCreator().getRandomNameForMock())
                .withConnectionUserName("user")
                .withConnectionPassword("password")
                .withConnectionURL("jdbc:postgresql://somedb.com:5432/mydb")
                .withType("HIVE");
    }

    public RdsConfigEntity withRequest(RDSConfigRequest request) {
        setRequest(request);
        return this;
    }

    public RdsConfigEntity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public RdsConfigEntity withConnectionPassword(String password) {
        getRequest().setConnectionPassword(password);
        return this;
    }

    public RdsConfigEntity withConnectionUserName(String username) {
        getRequest().setConnectionUserName(username);
        return this;
    }

    public RdsConfigEntity withConnectionURL(String connectionURL) {
        getRequest().setConnectionURL(connectionURL);
        return this;
    }

    public RdsConfigEntity withType(String type) {
        getRequest().setType(type);
        return this;
    }

    public RdsTestResult getResponseTestResult() {
        return response;
    }

    public void setResponseTestResult(RdsTestResult response) {
        this.response = response;
    }
}