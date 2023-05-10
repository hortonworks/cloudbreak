package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.cloudbreak.rotation.secret.type.CloudbreakSecretType.CLOUDBREAK_CM_ADMIN_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.secret.type.CloudbreakSecretType.MGMT_CM_ADMIN_PASSWORD;

import java.util.Set;

import javax.inject.Inject;

import org.testng.ITestContext;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.clouderamanager.ClouderaManagerUtil;

public class DistroXSecretRotationTest extends AbstractE2ETest {

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private ClouderaManagerUtil clouderaManagerUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultDatahub(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running default Distrox cluster",
            when = "CM admin user secrets are getting rotated",
            then = "rotation should be successful and cluster should be available")
    public void testDistroXCMAdminUserSecretRotation(TestContext testContext, ITestContext iTestContext) {
        testContext
                .given(DistroXTestDto.class)
                .when(distroXTestClient.rotateSecret(Set.of(MGMT_CM_ADMIN_PASSWORD, CLOUDBREAK_CM_ADMIN_PASSWORD)))
                .awaitForFlow()
                .validate();

    }
}
