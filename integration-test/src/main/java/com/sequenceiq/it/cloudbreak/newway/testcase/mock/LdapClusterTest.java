package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.springframework.http.HttpMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.newway.client.LdapTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.AmbariRepositoryV4TestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.AmbariTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.mock.model.AmbariMock;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class LdapClusterTest extends AbstractIntegrationTest {

    @Inject
    private LdapTestClient ldapTestClient;

    @Inject
    private StackTestClient stackTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid cluster request with ldap configuration",
            when = "calling create cluster",
            then = "the ldap should be configured on the cluster")
    public void testCreateClusterWithLdap(MockedTestContext testContext) {
        testContext.getModel().getAmbariMock().postSyncLdap();
        testContext.getModel().getAmbariMock().putConfigureLdap();
        testContext.given(LdapTestDto.class)
                .when(ldapTestClient.createV4())
                .given(AmbariRepositoryV4TestDto.class)
                .given(AmbariTestDto.class)
                .withAmbariRepoDetails()
                .given(ClusterTestDto.class)
                .withLdapConfig()
                .withAmbari()
                .given(StackTestDto.class)
                .withCluster()
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .then(MockVerification.verify(HttpMethod.POST, AmbariMock.LDAP_SYNC_EVENTS))
                .then(MockVerification.verify(HttpMethod.PUT, AmbariMock.LDAP_CONFIGURATION))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid cluster request with ldap configuration",
            when = "calling create cluster and then delete the attached ldap",
            then = "the ldap should be configured on the cluster but the ldap delete throw BadRequestException because the ldap attached to the cluster")
    public void testTryToDeleteAttachedLdap(MockedTestContext testContext) {
        testContext.getModel().getAmbariMock().postSyncLdap();
        testContext.getModel().getAmbariMock().putConfigureLdap();

        String stackName = resourcePropertyProvider().getName();
        String deleteFail = resourcePropertyProvider().getName();
        String ldapName = resourcePropertyProvider().getName();

        testContext.given(LdapTestDto.class).withName(ldapName)
                .when(ldapTestClient.createV4())
                .given(AmbariRepositoryV4TestDto.class)
                .given(AmbariTestDto.class)
                .withAmbariRepoDetails()
                .given(ClusterTestDto.class)
                .withLdapConfig()
                .withAmbari()
                .given(StackTestDto.class)
                .withCluster()
                .withName(stackName)
                .when(stackTestClient.createV4())
                .given(LdapTestDto.class)
                .when(ldapTestClient.deleteV4(), key(deleteFail))
                .expect(BadRequestException.class, expectedMessage(String.format("LDAP config '%s' cannot be deleted "
                        + "because there are clusters associated with it: \\[%s\\].", ldapName, stackName)).withKey(deleteFail))
                .validate();
    }
}
