package com.sequenceiq.it.cloudbreak.newway.testcase;

import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class CredentialTest extends AbstractIntegrationTest {

    @Inject
    private CredentialTestClient credentialTestClient;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(Object[] data) {
        ((TestContext) data[0]).cleanupTestContextEntity();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateValidCredential(TestContext testContext) {
        String name = getNameGenerator().getRandomNameForMock();
        testContext.given(CredentialEntity.class)
                .withName(name)
                .when(credentialTestClient.post())
                .then((tc, entity, cc) -> {
                    assertNotNull(entity);
                    assertNotNull(entity.getResponse());
                    return entity;
                })
                .validate();
    }
}
