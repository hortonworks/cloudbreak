package com.sequenceiq.it.cloudbreak.testcase.mock;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.assertion.database.RedbeamsDatabaseTestAssertion;
import com.sequenceiq.it.cloudbreak.client.RedbeamsDatabaseTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class RedbeamsDatabaseTest extends AbstractIntegrationTest {

    @Inject
    private RedbeamsDatabaseTestClient databaseTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironment(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a prepared database",
            when = "the database is deleted and then a create request is sent with the same database name",
            then = "the database should be created again")
    public void createAndDeleteAndCreateWithSameNameThenShouldRecreatedDatabase(TestContext testContext) {
        String databaseName = resourcePropertyProvider().getName();
        testContext
                .given(RedbeamsDatabaseTestDto.class)
                .withName(databaseName)
                .when(databaseTestClient.createV4(), RunningParameter.key(databaseName))
                .when(databaseTestClient.listV4(), RunningParameter.key(databaseName))
                .then(RedbeamsDatabaseTestAssertion.containsDatabaseName(databaseName, 1), RunningParameter.key(databaseName))
                .when(databaseTestClient.deleteV4(), RunningParameter.key(databaseName))
                .when(databaseTestClient.listV4(), RunningParameter.key(databaseName))
                .then(RedbeamsDatabaseTestAssertion.containsDatabaseName(databaseName, 0), RunningParameter.key(databaseName))
                .when(databaseTestClient.createV4(), RunningParameter.key(databaseName))
                .when(databaseTestClient.listV4(), RunningParameter.key(databaseName))
                .then(RedbeamsDatabaseTestAssertion.containsDatabaseName(databaseName, 1), RunningParameter.key(databaseName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a prepared database",
            when = "when a database create request is sent with the same database name",
            then = "the create should return a BadRequestException")
    public void createAndCreateWithSameNameThenShouldThrowBadRequestException(TestContext testContext) {
        String databaseName = resourcePropertyProvider().getName();
        testContext
                .given(RedbeamsDatabaseTestDto.class)
                .withName(databaseName)
                .when(databaseTestClient.createV4(), RunningParameter.key(databaseName))
                .when(databaseTestClient.listV4(), RunningParameter.key(databaseName))
                .then(RedbeamsDatabaseTestAssertion.containsDatabaseName(databaseName, 1), RunningParameter.key(databaseName))
                .when(databaseTestClient.createV4(), RunningParameter.key(databaseName))
                .expect(BadRequestException.class, RunningParameter.key(databaseName))
                .validate();
    }
}
