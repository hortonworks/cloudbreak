package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.response.ManagementPackV4Response;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.newway.action.mpack.MpackTestAction;
import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.mpack.MPackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class ManagementPackTest extends AbstractIntegrationTest {

    private static final String SAME_NAME = "mockmpackname";

    private static final String ANOTHER_MPACK = "ANOTHER_MANAGEMENTPACK";

    private static final String FORBIDDEN = "FORBIDDEN";

    @BeforeMethod
    public void prepareUser(Object[] objects) {
        createDefaultUser((TestContext) objects[0]);
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
                .when(MpackTestAction::create)
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
        String name = getNameGenerator().getRandomNameForResource();
        String generatedKey = getNameGenerator().getRandomNameForResource();

        testContext
                .given(MPackTestDto.class)
                .withName(name)
                .when(MpackTestAction::create)
                .given(MPackTestDto.class)
                .withName(name)
                .when(MpackTestAction::create, key(generatedKey))
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
                .when(MpackTestAction::create)
                .when(MpackTestAction::delete)
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
        String generatedKey = getNameGenerator().getRandomNameForResource();

        testContext
                .given(MPackTestDto.class)
                .when(MpackTestAction::delete, key(generatedKey))
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
                .when(MpackTestAction::list)
                .validate();
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        ((TestContext) data[0]).cleanupTestContextEntity();
    }

    private AssertionV2<MPackTestDto> assertMpackExist() {
        return (testContext, entity, cloudbreakClient) -> {
            Long workspaceId = cloudbreakClient.getWorkspaceId();
            ManagementPackV4Response response;
            try {
                response = cloudbreakClient.getCloudbreakClient().managementPackV4Endpoint().getByNameInWorkspace(workspaceId, entity.getName());
            } catch (Exception e) {
                TestFailException testFailException =  new TestFailException("Couldn't find mpack");
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
