package com.sequenceiq.it.cloudbreak.testcase.mock;

import jakarta.inject.Inject;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.kerberos.model.create.ActiveDirectoryKerberosDescriptor;
import com.sequenceiq.freeipa.api.v1.kerberos.model.create.CreateKerberosConfigRequest;
import com.sequenceiq.freeipa.api.v1.kerberos.model.create.FreeIpaKerberosDescriptor;
import com.sequenceiq.freeipa.api.v1.kerberos.model.create.MITKerberosDescriptor;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;

public class KerberosConfigTest extends AbstractMockTest {

    private static final String SALT_HIGHSTATE = "state.highstate";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Test(dataProvider = "dataProviderForTest")
    public void testClusterCreationWithValidKerberos(MockedTestContext testContext, String blueprintName, KerberosTestData testData,
            @Description TestCaseDescription testCaseDescription) {
        CreateKerberosConfigRequest request = testData.getRequest();
        request.setName(extendNameWithGeneratedPart(request.getName()));
        testContext
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .awaitForFlow()
                .given(DistroXTestDto.class)
                .when(distroXTestClient.create())
                .awaitForFlow()
                .enableVerification()
                .await(STACK_AVAILABLE)
                .mockSalt().run().post().bodyContains(SALT_HIGHSTATE, 1).atLeast(1).verify()
                .mockCm().cmImportClusterTemplate().post().atLeast(1).verify()
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a cluster setup without kerberosname",
            when = "calling cluster creation",
            then = "the cluster should not been kerberized")
    public void testClusterCreationAttemptWithKerberosConfigWithoutName(MockedTestContext testContext) {
        testContext
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .awaitForFlow()
                .given(DistroXTestDto.class)
                .when(distroXTestClient.create())
                .awaitForFlow()
                .await(STACK_AVAILABLE)
                .validate();
    }

    @DataProvider(name = "dataProviderForTest")
    public Object[][] provide() {
        return new Object[][]{
                {
                        getBean(MockedTestContext.class),
                        resourcePropertyProvider().getName(),
                        KerberosTestData.FREEIPA,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("a valid stack request and a FreeIPA based kerberos configuration")
                                .when("calling create kerberos configuration and a cluster creation with that kerberos configuration")
                                .then("the cluster should be available")
                },
                {
                        getBean(MockedTestContext.class),
                        resourcePropertyProvider().getName(),
                        KerberosTestData.ACTIVE_DIRECTORY,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("a valid stack request and a Active Directory based kerberos configuration")
                                .when("calling create kerberos configuration and a cluster creation with that kerberos configuration")
                                .then("the cluster should be available")
                },
                {
                        getBean(MockedTestContext.class),
                        resourcePropertyProvider().getName(),
                        KerberosTestData.MIT,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("a valid stack request and a MIT based kerberos configuration")
                                .when("calling create kerberos configuration and a cluster creation with that kerberos configuration")
                                .then("the cluster should be available")
                }
        };
    }

    private String extendNameWithGeneratedPart(String name) {
        return String.format("%s-%s", name, resourcePropertyProvider().getName());
    }

    private enum KerberosTestData {

        ACTIVE_DIRECTORY {

            @Override
            public CreateKerberosConfigRequest getRequest() {
                CreateKerberosConfigRequest request = new CreateKerberosConfigRequest();
                request.setName("adKerberos");
                ActiveDirectoryKerberosDescriptor activeDirectory = new ActiveDirectoryKerberosDescriptor();
                activeDirectory.setTcpAllowed(true);
                activeDirectory.setPrincipal("admin/principal");
                activeDirectory.setPassword("kerberosPassword");
                activeDirectory.setUrl("someurl.com");
                activeDirectory.setAdminUrl("admin.url.com");
                activeDirectory.setRealm("realm");
                activeDirectory.setLdapUrl("otherurl.com");
                activeDirectory.setContainerDn("{}");
                request.setActiveDirectory(activeDirectory);
                return request;
            }
        },

        MIT {

            @Override
            public CreateKerberosConfigRequest getRequest() {
                CreateKerberosConfigRequest request = new CreateKerberosConfigRequest();
                request.setName("mitKerberos");
                MITKerberosDescriptor mit = new MITKerberosDescriptor();
                mit.setTcpAllowed(true);
                mit.setPrincipal("kerberosPrincipal");
                mit.setPassword("kerberosPassword");
                mit.setUrl("kerberosproviderurl.com");
                mit.setAdminUrl("kerberosadminurl.com");
                mit.setRealm("kerbRealm");
                request.setMit(mit);
                return request;
            }
        },

        FREEIPA {

            @Override
            public CreateKerberosConfigRequest getRequest() {
                CreateKerberosConfigRequest request = new CreateKerberosConfigRequest();
                FreeIpaKerberosDescriptor freeIpaRequest = new FreeIpaKerberosDescriptor();
                freeIpaRequest.setAdminUrl("http://someurl.com");
                freeIpaRequest.setRealm("someRealm");
                freeIpaRequest.setUrl("http://someadminurl.com");
                freeIpaRequest.setRealm("realm");
                freeIpaRequest.setPassword("freeipapassword");
                freeIpaRequest.setPrincipal("kerberosPrincipal");
                request.setName("freeIpaTest");
                request.setDescription("some free ipa description");
                request.setFreeIpa(freeIpaRequest);
                return request;
            }
        };

        public abstract CreateKerberosConfigRequest getRequest();
    }
}
