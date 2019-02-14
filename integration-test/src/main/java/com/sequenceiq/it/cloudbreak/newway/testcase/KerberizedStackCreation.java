package com.sequenceiq.it.cloudbreak.newway.testcase;

import org.springframework.http.HttpMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.action.KerberosPostAction;
import com.sequenceiq.it.cloudbreak.newway.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.KerberosEntity;
import com.sequenceiq.it.cloudbreak.newway.mock.model.SaltMock;

public class KerberizedStackCreation extends AbstractIntegrationTest {

    private static final String TEST_CONTEXT = "testContext";

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        minimalSetupForClusterCreation((TestContext) data[0]);
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        ((TestContext) data[0]).cleanupTestContextEntity();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreationWitKerberosAndWithoutCustomDomain(TestContext testContext) {
        testContext
                .given(KerberosEntity.class)
                .withDefaultAD()
                .when(KerberosPostAction.create())
                .given(ClusterEntity.class).withKerberos()
                .given(StackEntity.class).withCluster()
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .then(this::validateCustomDomain)
                .then(MockVerification.verify(HttpMethod.POST, SaltMock.SALT_ACTION_DISTRIBUTE).exactTimes(1).bodyContains(".realm.example.com", 5))
                .validate();
    }

    private StackEntity validateCustomDomain(TestContext testContext, StackEntity entity, CloudbreakClient cloudbreakClient) {
        if (!"realm.example.com".equals(entity.getResponse().getCustomDomains().getDomainName())) {
            throw new IllegalArgumentException("Custom domain name is not equals with kerberos realm");
        }
        return entity;
    }
}
