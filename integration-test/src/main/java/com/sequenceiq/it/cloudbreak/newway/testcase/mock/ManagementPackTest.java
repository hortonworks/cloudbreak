package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.response.ManagementPackV4Response;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.client.MpackTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.mpack.MPackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class ManagementPackTest extends AbstractIntegrationTest {

    private static final String SAME_NAME = "mockmpackname";

    private static final String ANOTHER_MPACK = "ANOTHER_MANAGEMENTPACK";

    private static final String FORBIDDEN = "FORBIDDEN";

    @Inject
    private MpackTestClient mpackTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "a valid mpack request",
            when = "calling create mpack",
            then = "getting back mpack in mpack list")
    public void testMpackCreation(TestContext testContext) {
        createDefaultUser(testContext);
        testContext
                .given(MPackTestDto.class)
                .when(mpackTestClient.createV4())
                .then(assertMpackExist())
                .validate();

    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "an mpack which exist in the database and valid mpack request with the same name",
            when = "calling create mpack",
            then = "getting BadRequestException")
    public void testMpackCreateWithSameName(TestContext testContext) {
        createDefaultUser(testContext);
        String name = resourcePropertyProvider().getName();
        String generatedKey = resourcePropertyProvider().getName();

        testContext
                .given(MPackTestDto.class)
                .withName(name)
                .when(mpackTestClient.createV4())
                .given(MPackTestDto.class)
                .withName(name)
                .when(mpackTestClient.createV4(), key(generatedKey))
                .expect(BadRequestException.class, key(generatedKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "an mpack which exist in the database",
            when = "delete mpack with the specified name",
            then = "getting the list of mpack without that specific mpack")
    public void testMpackDeletion(TestContext testContext) {
        createDefaultUser(testContext);
        testContext
                .given(MPackTestDto.class)
                .when(mpackTestClient.createV4())
                .when(mpackTestClient.deleteV4())
                .then(assertMpackNotExist())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "an mpack which does not exist in the database",
            when = "delete mpack with the specified name",
            then = "getting the list of mpack without that specific mpack")
    public void testDeleteWhenNotExist(TestContext testContext) {
        createDefaultUser(testContext);
        String generatedKey = resourcePropertyProvider().getName();

        testContext
                .given(MPackTestDto.class)
                .when(mpackTestClient.deleteV4(), key(generatedKey))
                .expect(ForbiddenException.class, key(generatedKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "mpacks which are in the database",
            when = "list mpacks",
            then = "getting the list of mpacks")
    public void testMpackGetAll(TestContext testContext) {
        createDefaultUser(testContext);
        testContext
                .given(MPackTestDto.class)
                .when(mpackTestClient.listV4())
                .validate();
    }

    private AssertionV2<MPackTestDto> assertMpackExist() {
        return (testContext, entity, cloudbreakClient) -> {
            Long workspaceId = cloudbreakClient.getWorkspaceId();
            ManagementPackV4Response response;
            try {
                response = cloudbreakClient.getCloudbreakClient().managementPackV4Endpoint().getByNameInWorkspace(workspaceId, entity.getName());
            } catch (Exception e) {
                TestFailException testFailException = new TestFailException("Couldn't find mpack");
                testFailException.initCause(e);
                throw testFailException;
            }
            entity.setResponse(response);
            return entity;
        };
    }

    private AssertionV2<MPackTestDto> assertMpackNotExist() {
        return (testContext, entity, cloudbreakClient) -> {
            Long workspaceId = cloudbreakClient.getWorkspaceId();
            ManagementPackV4Response response;
            try {
                response = cloudbreakClient.getCloudbreakClient().managementPackV4Endpoint().getByNameInWorkspace(workspaceId, entity.getName());
            } catch (Exception e) {
                return entity;
            }
            entity.setResponse(response);
            throw new TestFailException("Found ManagePack with name: " + entity.getName());
        };
    }
}
