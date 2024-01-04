package com.sequenceiq.it.cloudbreak.assertion.proxy;

import java.util.Locale;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.proxy.model.request.ProxyRequest;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.config.ProxyConfigProperties;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.proxy.ProxyTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.util.clouderamanager.ClouderaManagerUtil;

@Component
class ProxyConfigCmAssertions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigCmAssertions.class);

    @Inject
    private ProxyConfigProperties proxyConfigProperties;

    @Inject
    private ClouderaManagerUtil clouderaManagerUtil;

    public Assertion<SdxInternalTestDto, SdxClient> validateDatalakeCmNoProxySettings() {
        return (testContext, testDto, client) -> {
            LOGGER.info("Validating {} datalake's CM has no proxy settings", testDto.getCrn());
            Map<String, String> expectedConfig = getEmptyExpectedConfig();
            clouderaManagerUtil.checkConfig(testDto, testContext, expectedConfig);
            return testDto;
        };
    }

    public Assertion<DistroXTestDto, CloudbreakClient> validateDatahubCmNoProxySettings() {
        return (testContext, testDto, client) -> {
            LOGGER.info("Validating {} datahub's CM has no proxy settings", testDto.getCrn());
            Map<String, String> expectedConfig = getEmptyExpectedConfig();
            clouderaManagerUtil.checkConfig(testDto, testContext, expectedConfig);
            return testDto;
        };
    }

    public Assertion<SdxInternalTestDto, SdxClient> validateDatalakeCmProxySettings(ProxyTestDto proxy) {
        return (testContext, testDto, client) -> {
            LOGGER.info("Validating {} datalake's CM has correct proxy settings {}", testDto.getCrn(), proxy.getName());
            Map<String, String> expectedConfig = getExpectedConfig(proxy);
            clouderaManagerUtil.checkConfig(testDto, testContext, expectedConfig);
            return testDto;
        };
    }

    public Assertion<DistroXTestDto, CloudbreakClient> validateDatahubCmProxySettings(ProxyTestDto proxy) {
        return (testContext, testDto, client) -> {
            LOGGER.info("Validating {} datahub's CM has correct proxy settings {}", testDto.getCrn(), proxy.getName());
            Map<String, String> expectedConfig = getExpectedConfig(proxy);
            clouderaManagerUtil.checkConfig(testDto, testContext, expectedConfig);
            return testDto;
        };
    }

    private static Map<String, String> getEmptyExpectedConfig() {
        return getExpectedConfig("", "", "", "", "");
    }

    private static Map<String, String> getExpectedConfig(ProxyTestDto proxy) {
        ProxyRequest proxyRequest = proxy.getRequest();
        return getExpectedConfig(proxyRequest.getHost(), proxyRequest.getPort().toString(), proxyRequest.getProtocol().toUpperCase(Locale.ROOT),
                proxyRequest.getUserName(), proxyRequest.getNoProxyHosts());
    }

    private static Map<String, String> getExpectedConfig(String host, String port, String protocol, String user, String noProxy) {
        return Map.of(
                "parcel_proxy_server", host,
                "parcel_proxy_port", port,
                "parcel_proxy_protocol", protocol,
                "parcel_proxy_user", user,
                "parcel_no_proxy_list", noProxy
        );
    }
}
