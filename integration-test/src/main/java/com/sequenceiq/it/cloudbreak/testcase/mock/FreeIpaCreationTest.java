package com.sequenceiq.it.cloudbreak.testcase.mock;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.it.cloudbreak.assertion.freeipa.FreeIpaKerberosTestAssertion;
import com.sequenceiq.it.cloudbreak.assertion.freeipa.FreeIpaLdapTestAssertion;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;

public class FreeIpaCreationTest extends AbstractMockTest {

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironment(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "environment is present",
            when = "calling a freeipa creation",
            then = "freeipa should be available with kerberos and ldap config")
    public void testCreateFreeIpa(MockedTestContext testContext) {
        testContext
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .await(Status.AVAILABLE)
                .then(FreeIpaKerberosTestAssertion.validate())
                .then(FreeIpaLdapTestAssertion.validate())
                .validate();
    }
}
