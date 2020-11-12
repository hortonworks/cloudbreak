package com.sequenceiq.it.cloudbreak.testcase.mock;

import java.math.BigDecimal;

import javax.inject.Inject;

import org.springframework.http.HttpMethod;
import org.testng.annotations.Test;

import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.sequenceiq.it.cloudbreak.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.assertion.kerberos.KerberosTestAssertion;
import com.sequenceiq.it.cloudbreak.client.KerberosTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.kerberos.ActiveDirectoryKerberosDescriptorTestDto;
import com.sequenceiq.it.cloudbreak.dto.kerberos.FreeIpaKerberosDescriptorTestDto;
import com.sequenceiq.it.cloudbreak.dto.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.mock.model.ClouderaManagerMock;
import com.sequenceiq.it.cloudbreak.mock.model.SaltMock;
import com.sequenceiq.it.cloudbreak.spark.DynamicRouteStack;

public class KerberizedStackCreationTest extends AbstractMockTest {

    @Inject
    private KerberosTestClient kerberosTestClient;

    @Inject
    private StackTestClient stackTestClient;

    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironmentWithNetwork(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an ActiveDirectory based kerberos config without realm and domain",
            when = "calling a stack creation with that kerberos config",
            then = "custom should be used and salt action should be called")
    public void testCreationWitKerberosAndStackWithoutCustomDomainAndADWithoutDomainAndWithRealm(MockedTestContext testContext) {
        DynamicRouteStack dynamicRouteStack = testContext.getModel().getClouderaManagerMock().getDynamicRouteStack();
        dynamicRouteStack.put(ClouderaManagerMock.API_V31 + "/cm/config", (request, response) -> new ApiConfigList());
        dynamicRouteStack.post(ClouderaManagerMock.API_V31
                + "/cm/commands/importAdminCredentials", (request, response) -> new ApiCommand().id(new BigDecimal(1)));
        testContext
                .given(ActiveDirectoryKerberosDescriptorTestDto.class)
                .withDomain(null)
                .withRealm("realm.addomain.com")
                .given(KerberosTestDto.class)
                .withActiveDirectoryDescriptor()
                .when(kerberosTestClient.createV1())
                .given(ClusterTestDto.class)
                .given(StackTestDto.class)
                .withCluster()
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .then(KerberosTestAssertion.validateCustomDomain("realm.addomain.com"))
                .then(MockVerification.verify(HttpMethod.POST, SaltMock.SALT_ACTION_DISTRIBUTE).exactTimes(1).bodyContains("realm.addomain.com", 12))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an ActiveDirectory based kerberos config without realm and with domain",
            when = "calling a stack creation with that kerberos config",
            then = "propagated domain should be used and salt action should be called")
    public void testCreationWitKerberosAndStackWithoutCustomDomainAndADWithDomainAndWithRealm(MockedTestContext testContext) {
        DynamicRouteStack dynamicRouteStack = testContext.getModel().getClouderaManagerMock().getDynamicRouteStack();
        dynamicRouteStack.put(ClouderaManagerMock.API_V31 + "/cm/config", (request, response) -> new ApiConfigList());
        dynamicRouteStack
                .post(ClouderaManagerMock.API_V31
                        + "/cm/commands/importAdminCredentials", (request, response) -> new ApiCommand().id(new BigDecimal(1)));
        testContext
                .given(ActiveDirectoryKerberosDescriptorTestDto.class)
                .withDomain("custom.addomain.com")
                .withRealm("realm.addomain.com")
                .given(KerberosTestDto.class)
                .withActiveDirectoryDescriptor()
                .when(kerberosTestClient.createV1())
                .given(ClusterTestDto.class)
                .given(StackTestDto.class)
                .withCluster()
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .then(KerberosTestAssertion.validateCustomDomain("custom.addomain.com"))
                .then(MockVerification.verify(HttpMethod.POST, SaltMock.SALT_ACTION_DISTRIBUTE).exactTimes(1).bodyContains("custom.addomain.com", 12))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a FreeIPA based kerberos config with realm and without domain",
            when = "calling a stack creation with that kerberos config",
            then = "propagated realm should be used and salt action should be called")
    public void testCreationWitKerberosAndStackWithoutCustomDomainAndFreeIpaWithoutDomainAndWithRealm(MockedTestContext testContext) {
        DynamicRouteStack dynamicRouteStack = testContext.getModel().getClouderaManagerMock().getDynamicRouteStack();
        dynamicRouteStack.put(ClouderaManagerMock.API_V31 + "/cm/config", (request, response) -> new ApiConfigList());
        testContext
                .given(FreeIpaKerberosDescriptorTestDto.class)
                .withDomain(null)
                .withRealm("realm.freeiparealm.com")
                .given(KerberosTestDto.class)
                .withFreeIpaDescriptor()
                .when(kerberosTestClient.createV1())
                .given(ClusterTestDto.class)
                .given(StackTestDto.class)
                .withCluster()
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .then(KerberosTestAssertion.validateCustomDomain("realm.freeiparealm.com"))
                .then(MockVerification.verify(HttpMethod.POST, SaltMock.SALT_ACTION_DISTRIBUTE).exactTimes(1).bodyContains("realm.freeiparealm.com", 12))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a FreeIPA based kerberos config with realm and domain",
            when = "calling a stack creation with that kerberos config",
            then = "propagated domain should be used and salt action should be called")
    public void testCreationWitKerberosAndStackWithoutCustomDomainAndFreeIpaWithDomainAndWithRealm(MockedTestContext testContext) {
        DynamicRouteStack dynamicRouteStack = testContext.getModel().getClouderaManagerMock().getDynamicRouteStack();
        dynamicRouteStack.put(ClouderaManagerMock.API_V31 + "/cm/config", (request, response) -> new ApiConfigList());

        testContext
                .given(FreeIpaKerberosDescriptorTestDto.class)
                .withDomain("custom.freeipadomain.com")
                .withRealm("realm.freeiparealm.com")
                .given(KerberosTestDto.class)
                .withFreeIpaDescriptor()
                .when(kerberosTestClient.createV1())
                .given(ClusterTestDto.class)
                .given(StackTestDto.class)
                .withCluster()
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .then(KerberosTestAssertion.validateCustomDomain("custom.freeipadomain.com"))
                .then(MockVerification.verify(HttpMethod.POST, SaltMock.SALT_ACTION_DISTRIBUTE).exactTimes(1).bodyContains("custom.freeipadomain.com", 12))
                .validate();
    }
}
