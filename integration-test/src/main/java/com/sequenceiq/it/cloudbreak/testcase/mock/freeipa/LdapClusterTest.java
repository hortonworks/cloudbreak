package com.sequenceiq.it.cloudbreak.testcase.mock.freeipa;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.http.HttpMethod;
import org.testng.annotations.Test;

import com.cloudera.api.swagger.model.ApiAuthRoleMetadata;
import com.cloudera.api.swagger.model.ApiAuthRoleMetadataList;
import com.cloudera.api.swagger.model.ApiExternalUserMappingList;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.client.LdapTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.mock.model.ClouderaManagerMock;
import com.sequenceiq.it.cloudbreak.spark.DynamicRouteStack;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

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
        DynamicRouteStack dynamicRouteStack = testContext.getModel().getClouderaManagerMock().getDynamicRouteStack();
        dynamicRouteStack.get(ClouderaManagerMock.READ_AUTH_ROLES,
                (request, response) -> new ApiAuthRoleMetadataList().items(List.of(new ApiAuthRoleMetadata().role("ROLE_ADMIN"))));
        dynamicRouteStack.post(ClouderaManagerMock.API_ROOT + "/externalUserMappings",
                (request, response) -> new ApiExternalUserMappingList());

        testContext.given(LdapTestDto.class)
                .when(ldapTestClient.createV1())
                .given(ClusterTestDto.class)
                .given(StackTestDto.class)
                .withCluster()
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .then(externalUserMappingsCall(1))
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

    private static List<Assertion<StackTestDto, CloudbreakClient>> externalUserMappingsCall(int times) {
        List<Assertion<StackTestDto, CloudbreakClient>> verifications = new LinkedList<>();
        verifications.add(MockVerification.verify(HttpMethod.POST, ClouderaManagerMock.API_ROOT + "/externalUserMappings").exactTimes(times));
        return verifications;
    }
}
