package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;

import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnResourceDescriptor;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkMockParams;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.client.RedbeamsDatabaseServerTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseServerTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.redbeams.api.model.common.Status;

public class RedbeamsDatabaseServerTest extends AbstractMockTest {

    @Inject
    private RedbeamsDatabaseServerTestClient redbeamsDatabaseServerTest;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a prepared database",
            when = "when a database create request is sent with the same database name",
            then = "the create should return a BadRequestException")
    public void createRedbeamsDatabaseServerTest(MockedTestContext testContext) {
        String databaseName = resourcePropertyProvider().getName();
        String networkKey = "someOtherNetwork";
        String clusterCrn = Crn.builder(CrnResourceDescriptor.DATALAKE)
                .setResource(UUID.randomUUID().toString())
                .setAccountId("cloudera")
                .build()
                .toString();
        testContext
                .given(networkKey, EnvironmentNetworkTestDto.class)
                .withMock(new EnvironmentNetworkMockParams())
                .given(EnvironmentTestDto.class)
                .withNetwork(networkKey)
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(RedbeamsDatabaseServerTestDto.class)
                .withName(databaseName)
                .withClusterCrn(clusterCrn)
                .when(redbeamsDatabaseServerTest.createV4())
                .await(Status.AVAILABLE)
                .when(redbeamsDatabaseServerTest.deleteV4())
                .await(Status.DELETE_COMPLETED)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a prepared database create request",
            when = "when a database create request is sent with the invalid crn",
            then = "the create should return a BadRequestException")
    public void createRedbeamsDatabaseServerWithInvalidCrnTest(MockedTestContext testContext) {
        String databaseName = resourcePropertyProvider().getName();
        String networkKey = "someOtherNetwork";
        testContext
                .given(networkKey, EnvironmentNetworkTestDto.class)
                .withMock(new EnvironmentNetworkMockParams())
                .given(EnvironmentTestDto.class)
                .withNetwork(networkKey)
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(RedbeamsDatabaseServerTestDto.class)
                .withName(databaseName)
                .withClusterCrn(Crn.builder(CrnResourceDescriptor.ENVIRONMENT).setAccountId("acc").setResource("res").build().toString())
                .whenException(redbeamsDatabaseServerTest.createV4(), BadRequestException.class,
                        expectedMessage(".*Crn provided: crn:cdp:environments:us-west-1:acc:environment:res has invalid resource type or" +
                                " service type. Accepted service type / resource type pairs: [(]datalake,datalake[)],[(]datahub,cluster[)].*"))
                .validate();
    }
}
