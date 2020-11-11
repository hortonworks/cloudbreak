package com.sequenceiq.it.cloudbreak.testcase.mock;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.assertion.audit.KerberosConfigAuditGrpcServiceAssertion;
import com.sequenceiq.it.cloudbreak.assertion.kerberos.KerberosConfigListStructuredEventAssertions;
import com.sequenceiq.it.cloudbreak.client.KerberosTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.kerberos.KerberosTestDto;

public class KerberosConfigCrudTest extends AbstractMockTest {

    @Inject
    private KerberosTestClient kerberosTestClient;

    @Inject
    private KerberosConfigListStructuredEventAssertions kerberosConfigListStructuredEventAssertions;

    @Inject
    private KerberosConfigAuditGrpcServiceAssertion kerberosConfigAuditGrpcServiceAssertion;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironment(testContext);
        createDefaultFreeIpa(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "create/delete a kerberos config;",
            then = "validate the events")
    public void testCreateDeleteKerberosConfig(TestContext testContext) {
        testContext
                .given(KerberosTestDto.class)
                .when(kerberosTestClient.describeV1())
                .when(kerberosTestClient.deleteV1())
                .then(kerberosConfigListStructuredEventAssertions::checkDeleteEvents)
                .then(kerberosConfigAuditGrpcServiceAssertion::delete)
                .validate();
    }
}
