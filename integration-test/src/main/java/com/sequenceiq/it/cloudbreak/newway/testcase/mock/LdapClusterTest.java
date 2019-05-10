package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.newway.mock.model.ClouderaManagerMock.API_ROOT;
import static com.sequenceiq.it.cloudbreak.newway.mock.model.ClouderaManagerMock.READ_AUTH_ROLES;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.cloudera.api.swagger.model.ApiAuthRoleMetadata;
import com.cloudera.api.swagger.model.ApiAuthRoleMetadataList;
import com.cloudera.api.swagger.model.ApiExternalUserMappingList;
import com.sequenceiq.it.cloudbreak.newway.client.LdapTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.spark.DynamicRouteStack;

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
        dynamicRouteStack.get(READ_AUTH_ROLES,
                (request, response) -> new ApiAuthRoleMetadataList().items(List.of(new ApiAuthRoleMetadata().role("ROLE_ADMIN"))));
        dynamicRouteStack.post(API_ROOT + "/externalUserMappings",
                (request, response) -> new ApiExternalUserMappingList());

        testContext.given(LdapTestDto.class)
                .when(ldapTestClient.createV4())
                .given(ClusterTestDto.class)
                .withLdapConfig()
                .given(StackTestDto.class)
                .withCluster()
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid cluster request with ldap configuration",
            when = "calling create cluster and then delete the attached ldap",
            then = "the ldap should be configured on the cluster but the ldap delete throw BadRequestException because the ldap attached to the cluster")
    public void testTryToDeleteAttachedLdap(MockedTestContext testContext) {
        String stackName = resourcePropertyProvider().getName();
        String deleteFail = resourcePropertyProvider().getName();
        String ldapName = resourcePropertyProvider().getName();

        testContext.given(LdapTestDto.class).withName(ldapName)
                .when(ldapTestClient.createV4())
                .given(ClusterTestDto.class)
                .withLdapConfig()
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
