package com.sequenceiq.it.cloudbreak.testcase.e2e.proxy;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.assertion.proxy.ProxyConfigAssertions;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.ProxyTestClient;
import com.sequenceiq.it.cloudbreak.config.ProxyConfigProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.proxy.ProxyTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;

public class ModifyProxyConfigE2ETest extends AbstractE2ETest {

    private static final String PROXY2 = "proxy2";

    @Inject
    private ProxyConfigProperties proxyConfigProperties;

    @Inject
    private ProxyTestClient proxyTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private ProxyConfigAssertions proxyConfigAssertions;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createProxyConfig(testContext);
        createProxyConfig2(testContext);
        createDefaultEnvironment(testContext);
    }

    private void createProxyConfig2(TestContext testContext) {
        testContext
                .given(PROXY2, ProxyTestDto.class)
                    .withServerUser(proxyConfigProperties.getProxyUser2())
                    .withPassword(proxyConfigProperties.getProxyPassword2())
                .when(proxyTestClient.createIfNotExist())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running FreeIpa/Datalake/Datahub ",
            when = "adding/modifying/removing proxy config ",
            then = "all existing and newly created nodes should have the new proxy config settings "
    )
    public void testModifyProxyConfig(TestContext testContext) {
        ProxyTestDto proxy = testContext.given(ProxyTestDto.class);
        ProxyTestDto proxy2 = testContext.given(PROXY2, ProxyTestDto.class);

        addAndValidateProxy(testContext, proxy);
        createDatalakeWithoutDatabase(testContext);
        validateDatalakeProxy(testContext, proxy);

        changeAndValidateProxy(testContext, proxy2);
        createDefaultDatahubForExistingDatalake(testContext);
        validateDatahubProxy(testContext, proxy2);

        removeAndValidateProxy(testContext);
    }

    private void addAndValidateProxy(TestContext testContext, ProxyTestDto proxy) {
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.modifyProxyConfig(proxy.getName()))
                .await(EnvironmentStatus.PROXY_CONFIG_MODIFICATION_ON_FREEIPA_IN_PROGRESS)
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .then(proxyConfigAssertions.validateFreeIpaProxyConfig(proxy))
                .validate();
    }

    private void validateDatalakeProxy(TestContext testContext, ProxyTestDto proxy) {
        testContext
                .given(SdxInternalTestDto.class)
                .then(proxyConfigAssertions.validateDatalakeProxyConfig(proxy))
                .validate();
    }

    private void changeAndValidateProxy(TestContext testContext, ProxyTestDto proxy2) {
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.modifyProxyConfig(proxy2.getName()))
                .await(EnvironmentStatus.PROXY_CONFIG_MODIFICATION_ON_DATALAKE_IN_PROGRESS)
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .then(proxyConfigAssertions.validateFreeIpaProxyConfig(proxy2))
                .given(SdxInternalTestDto.class)
                .then(proxyConfigAssertions.validateDatalakeProxyConfig(proxy2))
                .validate();
    }

    private void validateDatahubProxy(TestContext testContext, ProxyTestDto proxy2) {
        testContext
                .given(DistroXTestDto.class)
                .then(proxyConfigAssertions.validateDatahubProxyConfig(proxy2))
                .validate();
    }

    private void removeAndValidateProxy(TestContext testContext) {
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.modifyProxyConfig(null))
                .await(EnvironmentStatus.PROXY_CONFIG_MODIFICATION_ON_DATAHUBS_IN_PROGRESS)
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .then(proxyConfigAssertions.validateFreeIpaNoProxyConfig())
                .given(SdxInternalTestDto.class)
                .then(proxyConfigAssertions.validateDatalakeNoProxyConfig())
                .given(DistroXTestDto.class)
                .then(proxyConfigAssertions.validateDatahubNoProxyConfig())
                .validate();
    }
}
