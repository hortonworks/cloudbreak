package com.sequenceiq.it.cloudbreak.testcase.mock;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.LdapTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;

public class LdapClusterTest extends AbstractMockTest {

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

        testContext
                .given(ClusterTestDto.class)
                .given(StackTestDto.class)
                .withCluster()
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .mockCm().externalUserMappings().post().times(1).verify()
                .validate();
    }

    // TODO: Delete the test or improve the validation!
//    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
//    @Description(
//            given = "a valid cluster request with ldap configuration",
//            when = "calling create cluster and then delete the attached ldap",
//            then = "the ldap should be configured on the cluster but the ldap delete throw BadRequestException because the ldap attached to the cluster")
//    public void testTryToDeleteAttachedLdap(MockedTestContext testContext) {
//        String stackName = resourcePropertyProvider().getName();
//        String deleteFail = resourcePropertyProvider().getName();
//        String ldapName = resourcePropertyProvider().getName();
//
//        testContext.given(LdapTestDto.class).withName(ldapName)
//                .when(ldapTestClient.createV1())
//                .given(ClusterTestDto.class)
//                .given(StackTestDto.class)
//                .withCluster()
//                .withName(stackName)
//                .when(stackTestClient.createV4())
//                .given(LdapTestDto.class)
//                .when(ldapTestClient.deleteV1(), RunningParameter.key(deleteFail))
//                .expect(BadRequestException.class, RunningParameter.expectedMessage(String.format("LDAP config '%s' cannot be deleted "
//                        + "because there are clusters associated with it: \\[%s\\].", ldapName, stackName)).withKey(deleteFail))
//                .validate();
//    }
}
