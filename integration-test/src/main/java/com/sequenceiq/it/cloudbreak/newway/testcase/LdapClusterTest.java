package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.springframework.http.HttpMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.newway.client.LdapConfigTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.AmbariEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.AmbariRepositoryV4Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.ldap.LdapConfigTestDto;
import com.sequenceiq.it.cloudbreak.newway.mock.model.AmbariMock;

public class LdapClusterTest extends AbstractIntegrationTest {

    @Inject
    private LdapConfigTestClient ldapConfigTestClient;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        minimalSetupForClusterCreation((TestContext) data[0]);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(Object[] data) {
        ((TestContext) data[0]).cleanupTestContextEntity();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateClusterWithLdap(MockedTestContext testContext) {
        testContext.getModel().getAmbariMock().postSyncLdap();
        testContext.getModel().getAmbariMock().putConfigureLdap();
        testContext.given(LdapConfigTestDto.class)
                .when(ldapConfigTestClient.post())
                .given(AmbariRepositoryV4Entity.class)
                .given(AmbariEntity.class).withAmbariRepoDetails()
                .given(ClusterEntity.class).withLdapConfig().withAmbari()
                .given(StackTestDto.class).withCluster()
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .then(MockVerification.verify(HttpMethod.POST, AmbariMock.LDAP_SYNC_EVENTS))
                .then(MockVerification.verify(HttpMethod.PUT, AmbariMock.LDAP_CONFIGURATION))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testTryToDeleteAttachedLdap(MockedTestContext testContext) {
        testContext.getModel().getAmbariMock().postSyncLdap();
        testContext.getModel().getAmbariMock().putConfigureLdap();

        String stackName = getNameGenerator().getRandomNameForMock();
        String ldapName = getNameGenerator().getRandomNameForMock();

        testContext.given(LdapConfigTestDto.class).withName(ldapName)
                .when(ldapConfigTestClient.post())
                .given(AmbariRepositoryV4Entity.class)
                .given(AmbariEntity.class).withAmbariRepoDetails()
                .given(ClusterEntity.class).withLdapConfig().withAmbari()
                .given(StackTestDto.class).withCluster().withName(stackName)
                .when(Stack.postV4())
                .given(LdapConfigTestDto.class)
                .when(ldapConfigTestClient.delete(), key("deleteFail"))
                .expect(BadRequestException.class, expectedMessage(String.format("LDAP config '%s' cannot be deleted "
                        + "because there are clusters associated with it: \\[%s\\].", ldapName, stackName)).withKey("deleteFail"))
                .validate();
    }
}
