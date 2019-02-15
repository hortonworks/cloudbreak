package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.it.cloudbreak.newway.assertion.kerberos.KerberosTestAssertion.validateCustomDomain;

import org.springframework.http.HttpMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.action.kerberos.KerberosTestAction;
import com.sequenceiq.it.cloudbreak.newway.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.kerberos.ActiveDirectoryKerberosDescriptorTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.kerberos.FreeIPAKerberosDescriptorTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.kerberos.KerberosTestDto;
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
    public void testCreationWitKerberosAndStackWithoutCustomDomainAndADWithoutDomainAndWithRealm(TestContext testContext) {
        testContext
                .given(ActiveDirectoryKerberosDescriptorTestDto.class).withDomain(null).withRealm("realm.addomain.com")
                .given(KerberosTestDto.class).withActiveDirectoryDescriptor()
                .when(KerberosTestAction::post)
                .given(ClusterEntity.class).withKerberos()
                .given(StackEntity.class).withCluster()
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .then(validateCustomDomain("realm.addomain.com"))
                .then(MockVerification.verify(HttpMethod.POST, SaltMock.SALT_ACTION_DISTRIBUTE).exactTimes(1).bodyContains(".realm.addomain.com", 5))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreationWitKerberosAndStackWithoutCustomDomainAndADWithDomainAndWithRealm(TestContext testContext) {
        testContext
                .given(ActiveDirectoryKerberosDescriptorTestDto.class).withDomain("custom.addomain.com").withRealm("realm.addomain.com")
                .given(KerberosTestDto.class).withActiveDirectoryDescriptor()
                .when(KerberosTestAction::post)
                .given(ClusterEntity.class).withKerberos()
                .given(StackEntity.class).withCluster()
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .then(validateCustomDomain("custom.addomain.com"))
                .then(MockVerification.verify(HttpMethod.POST, SaltMock.SALT_ACTION_DISTRIBUTE).exactTimes(1).bodyContains(".custom.addomain.com", 5))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreationWitKerberosAndStackWithoutCustomDomainAndFreeIPAWithoutDomainAndWithRealm(TestContext testContext) {
        testContext
                .given(FreeIPAKerberosDescriptorTestDto.class).withDomain(null).withRealm("realm.freeiparealm.com")
                .given(KerberosTestDto.class).withFreeIPADescriptor()
                .when(KerberosTestAction::post)
                .given(ClusterEntity.class).withKerberos()
                .given(StackEntity.class).withCluster()
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .then(validateCustomDomain("realm.freeiparealm.com"))
                .then(MockVerification.verify(HttpMethod.POST, SaltMock.SALT_ACTION_DISTRIBUTE).exactTimes(1).bodyContains(".realm.freeiparealm.com", 5))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreationWitKerberosAndStackWithoutCustomDomainAndFreeIPAWithDomainAndWithRealm(TestContext testContext) {
        testContext
                .given(FreeIPAKerberosDescriptorTestDto.class).withDomain("custom.freeipadomain.com").withRealm("realm.freeiparealm.com")
                .given(KerberosTestDto.class).withFreeIPADescriptor()
                .when(KerberosTestAction::post)
                .given(ClusterEntity.class).withKerberos()
                .given(StackEntity.class).withCluster()
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .then(validateCustomDomain("custom.freeipadomain.com"))
                .then(MockVerification.verify(HttpMethod.POST, SaltMock.SALT_ACTION_DISTRIBUTE).exactTimes(1).bodyContains(".custom.freeipadomain.com", 5))
                .validate();
    }
}
