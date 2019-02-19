package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.response.ManagementPackV4Response;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.mpack.MpackTestAction;
import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.mpack.MPackTestDto;

public class ManagementPackTest extends AbstractIntegrationTest {

    private static final String SAME_NAME = "mockmpackname";

    private static final String ANOTHER_MPACK = "ANOTHER_MANAGEMENTPACK";

    private static final String FORBIDDEN = "FORBIDDEN";

    private static final String CREATED = "GIVEN_MPACK";

    @BeforeMethod
    public void prepareUser(Object[] objects) {
        createDefaultUser((TestContext) objects[0]);
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testMpackCreation(TestContext testContext) {
        createDefaultUser(testContext);
        testContext
                .given(MPackTestDto.class)
                .when(MpackTestAction::create)
                .then(assertMpackExist())
                .validate();

    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testMpackCreateWithSameName(TestContext testContext) {
        createDefaultUser(testContext);
        testContext
                .given(MPackTestDto.class).withName(SAME_NAME)
                .when(MpackTestAction::create)
                .given(MPackTestDto.class).withName(SAME_NAME)
                .when(MpackTestAction::create, key(ANOTHER_MPACK))
                .expect(BadRequestException.class, key(ANOTHER_MPACK))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
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
    public void testDeleteWhenNotExist(TestContext testContext) {
        createDefaultUser(testContext);
        testContext
                .given(MPackTestDto.class)
                .when(MpackTestAction::delete, key(FORBIDDEN))
                .expect(ForbiddenException.class, key(FORBIDDEN))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testMpackGetAll(TestContext testContext) {
        createDefaultUser(testContext);
        testContext
                .given(MPackTestDto.class)
                .when(MpackTestAction::list)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testMpackGetAllHasGivenMpack(TestContext testContext) {
        createDefaultUser(testContext);
        testContext
                .given(MPackTestDto.class)
                .when(MpackTestAction::create)
                .when(MpackTestAction::list)
                .then(assertMpacksHasGiven())
                .validate();
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        ((TestContext) data[0]).cleanupTestContextEntity();
    }

    private AssertionV2<MPackTestDto> assertMpackExist() {
        return new AssertionV2<MPackTestDto>() {
            @Override
            public MPackTestDto doAssertion(TestContext testContext, MPackTestDto entity, CloudbreakClient cloudbreakClient) throws Exception {
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
            }
        };
    }

    private AssertionV2<MPackTestDto> assertMpacksHasGiven() {
        return new AssertionV2<MPackTestDto>() {
            @Override
            public MPackTestDto doAssertion(TestContext testContext, MPackTestDto entity, CloudbreakClient cloudbreakClient) throws Exception {
                Assert.assertTrue(entity.getResponses().stream().anyMatch(mpack -> mpack.getId().equals(entity.getResponse().getId())));

                return entity;
            }
        };
    }

    private AssertionV2<MPackTestDto> assertMpackNotExist() {
        return new AssertionV2<MPackTestDto>() {
            @Override
            public MPackTestDto doAssertion(TestContext testContext, MPackTestDto entity, CloudbreakClient cloudbreakClient) throws Exception {
                Long workspaceId = cloudbreakClient.getWorkspaceId();
                ManagementPackV4Response response;
                try {
                    response = cloudbreakClient.getCloudbreakClient().managementPackV4Endpoint().getByNameInWorkspace(workspaceId, entity.getName());
                } catch (Exception e) {
                    return entity;
                }
                entity.setResponse(response);
                TestFailException testFailException =  new TestFailException("Found ManagePack with name: " + entity.getName());
                throw testFailException;
            }
        };
    }
}
