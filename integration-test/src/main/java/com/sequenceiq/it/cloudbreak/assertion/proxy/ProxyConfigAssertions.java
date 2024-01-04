package com.sequenceiq.it.cloudbreak.assertion.proxy;

import static com.sequenceiq.it.cloudbreak.assertion.CBAssertion.multiAssert;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.proxy.ProxyTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;

@Component
public class ProxyConfigAssertions {

    @Inject
    private ProxyConfigFileAssertions proxyConfigFileAssertions;

    @Inject
    private ProxyConfigUserDataAssertions proxyConfigUserDataAssertions;

    @Inject
    private ProxyConfigCmAssertions proxyConfigCmAssertions;

    public Assertion<FreeIpaTestDto, FreeIpaClient> validateFreeIpaNoProxyConfig() {
        return multiAssert(
                proxyConfigFileAssertions.validateFreeIpaNoProxyConfigFile(),
                proxyConfigUserDataAssertions.validateFreeIpaUserDataNoProxySettings()
        );
    }

    public Assertion<FreeIpaTestDto, FreeIpaClient> validateFreeIpaProxyConfig(ProxyTestDto proxy) {
        return multiAssert(
                proxyConfigFileAssertions.validateFreeIpaProxyConfigFile(proxy),
                proxyConfigUserDataAssertions.validateFreeIpaUserDataProxySettings(proxy)
        );
    }

    public Assertion<SdxInternalTestDto, SdxClient> validateDatalakeNoProxyConfig() {
        return multiAssert(
                proxyConfigFileAssertions.validateDatalakeNoProxyConfigFile(),
                proxyConfigUserDataAssertions.validateDatalakeUserDataNoProxySettings(),
                proxyConfigCmAssertions.validateDatalakeCmNoProxySettings()
        );
    }

    public Assertion<SdxInternalTestDto, SdxClient> validateDatalakeProxyConfig(ProxyTestDto proxy) {
        return multiAssert(
                proxyConfigFileAssertions.validateDatalakeProxyConfigFile(proxy),
                proxyConfigUserDataAssertions.validateDatalakeUserDataProxySettings(proxy),
                proxyConfigCmAssertions.validateDatalakeCmProxySettings(proxy)
        );
    }

    public Assertion<DistroXTestDto, CloudbreakClient> validateDatahubNoProxyConfig() {
        return multiAssert(
                proxyConfigFileAssertions.validateDatahubNoProxyConfigFile(),
                proxyConfigUserDataAssertions.validateDatahubUserDataNoProxySettings(),
                proxyConfigCmAssertions.validateDatahubCmNoProxySettings()
        );
    }

    public Assertion<DistroXTestDto, CloudbreakClient> validateDatahubProxyConfig(ProxyTestDto proxy) {
        return multiAssert(
                proxyConfigFileAssertions.validateDatahubProxyConfigFile(proxy),
                proxyConfigUserDataAssertions.validateDatahubUserDataProxySettings(proxy),
                proxyConfigCmAssertions.validateDatahubCmProxySettings(proxy)
        );
    }
}
