package com.sequenceiq.it.cloudbreak.testcase.e2e.proxy;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.Optional;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.assertion.proxy.ProxyConfigAssertions;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.ProxyTestClient;
import com.sequenceiq.it.cloudbreak.config.ProxyConfigProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.proxy.ProxyTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.hybrid.HybridCloudE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;

public class ModifyProxyConfigHybridCloudE2ETest extends HybridCloudE2ETest {

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
        super.setupTest(testContext);
        createProxyConfig(testContext);
        createProxyConfig2(testContext);
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
    public void testHybridModifyProxyConfig(TestContext testContext) {
        ProxyTestDto proxy = testContext.given(ProxyTestDto.class);
        ProxyTestDto proxy2 = testContext.given(PROXY2, ProxyTestDto.class);

        addAndValidateProxy(testContext, proxy);
        createChildDatalake(testContext);
        validateDatalakeProxy(testContext, proxy);

        changeAndValidateProxy(testContext, proxy2);
        createChildDatahubForExistingDatalake(testContext);
        validateDatahubProxy(testContext, proxy2);

        removeAndValidateProxy(testContext);
    }

    private void addAndValidateProxy(TestContext testContext, ProxyTestDto proxy) {
        modifyProxyOfBothEnvironments(testContext, proxy, EnvironmentStatus.PROXY_CONFIG_MODIFICATION_ON_FREEIPA_IN_PROGRESS);
        testContext
                .given(FreeIpaTestDto.class)
                .then(proxyConfigAssertions.validateFreeIpaProxyConfig(proxy))
                .validate();
    }

    private void validateDatalakeProxy(TestContext testContext, ProxyTestDto proxy) {
        testContext
                .given(CHILD_SDX_KEY, SdxInternalTestDto.class, CHILD_CLOUD_PLATFORM)
                .then(proxyConfigAssertions.validateDatalakeProxyConfig(proxy))
                .validate();
    }

    private void changeAndValidateProxy(TestContext testContext, ProxyTestDto proxy2) {
        modifyProxyOfBothEnvironments(testContext, proxy2, EnvironmentStatus.PROXY_CONFIG_MODIFICATION_ON_DATALAKE_IN_PROGRESS);
        testContext
                .given(FreeIpaTestDto.class)
                .then(proxyConfigAssertions.validateFreeIpaProxyConfig(proxy2))
                .given(CHILD_SDX_KEY, SdxInternalTestDto.class, CHILD_CLOUD_PLATFORM)
                .then(proxyConfigAssertions.validateDatalakeProxyConfig(proxy2))
                .validate();
    }

    private void validateDatahubProxy(TestContext testContext, ProxyTestDto proxy2) {
        testContext
                .given(CHILD_DISTROX_KEY, DistroXTestDto.class, CHILD_CLOUD_PLATFORM)
                .then(proxyConfigAssertions.validateDatahubProxyConfig(proxy2))
                .validate();
    }

    private void removeAndValidateProxy(TestContext testContext) {
        modifyProxyOfBothEnvironments(testContext, null, EnvironmentStatus.PROXY_CONFIG_MODIFICATION_ON_DATAHUBS_IN_PROGRESS);
        testContext
                .given(FreeIpaTestDto.class)
                .then(proxyConfigAssertions.validateFreeIpaNoProxyConfig())
                .given(CHILD_SDX_KEY, SdxInternalTestDto.class, CHILD_CLOUD_PLATFORM)
                .then(proxyConfigAssertions.validateDatalakeNoProxyConfig())
                .given(CHILD_DISTROX_KEY, DistroXTestDto.class, CHILD_CLOUD_PLATFORM)
                .then(proxyConfigAssertions.validateDatahubNoProxyConfig())
                .validate();
    }

    private void modifyProxyOfBothEnvironments(TestContext testContext, ProxyTestDto proxy, EnvironmentStatus awaitStatusOnChildEnvironment) {
        modifyProxyOfParentEnvironment(testContext, proxy);
        modifyProxyOfChildEnvironment(testContext, proxy, awaitStatusOnChildEnvironment);
    }

    private void modifyProxyOfParentEnvironment(TestContext testContext, ProxyTestDto proxy) {
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.modifyProxyConfig(getProxyConfigName(proxy)))
                .await(EnvironmentStatus.PROXY_CONFIG_MODIFICATION_ON_FREEIPA_IN_PROGRESS)
                .await(EnvironmentStatus.AVAILABLE)
                .validate();
    }

    private void modifyProxyOfChildEnvironment(TestContext testContext, ProxyTestDto proxy, EnvironmentStatus awaitStatus) {
        testContext
                .given(CHILD_ENVIRONMENT_KEY, EnvironmentTestDto.class, CHILD_CLOUD_PLATFORM)
                .when(environmentTestClient.modifyProxyConfig(getProxyConfigName(proxy)), key(CHILD_ENVIRONMENT_KEY))
                .await(awaitStatus, key(CHILD_ENVIRONMENT_KEY))
                .await(EnvironmentStatus.AVAILABLE, key(CHILD_ENVIRONMENT_KEY))
                .validate();
    }

    private String getProxyConfigName(ProxyTestDto proxy) {
        return Optional.ofNullable(proxy).map(AbstractTestDto::getName).orElse(null);
    }
}
