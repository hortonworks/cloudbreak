package com.sequenceiq.it.cloudbreak.assertion.proxy;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.proxy.model.request.ProxyRequest;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.proxy.ProxyTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;

@Component
class ProxyConfigUserDataAssertions {

    private static final String EXPORT_FORMAT = "export %s=%s";

    private static final String EXPORT_QUOTED_FORMAT = "export %s=\"%s\"";

    public Assertion<FreeIpaTestDto, FreeIpaClient> validateFreeIpaUserDataProxySettings(ProxyTestDto proxy) {
        return (testContext, testDto, client) -> {
            String stackNamePrefix = getFreeipaStackNamePrefix(testContext);
            validateUserDataProxySettings(testContext, stackNamePrefix, proxy);
            return testDto;
        };
    }

    public Assertion<SdxInternalTestDto, SdxClient> validateDatalakeUserDataProxySettings(ProxyTestDto proxy) {
        return (testContext, testDto, client) -> {
            validateUserDataProxySettings(testContext, testDto.getName(), proxy);
            return testDto;
        };
    }

    public Assertion<DistroXTestDto, CloudbreakClient> validateDatahubUserDataProxySettings(ProxyTestDto proxy) {
        return (testContext, testDto, client) -> {
            validateUserDataProxySettings(testContext, testDto.getName(), proxy);
            return testDto;
        };
    }

    private void validateUserDataProxySettings(TestContext testContext, String stackNamePrefix, ProxyTestDto proxy) {
        CloudFunctionality cloudFunctionality = testContext.getCloudProvider().getCloudFunctionality();
        Map<String, String> launchTemplateUserData = cloudFunctionality.getLaunchTemplateUserData(stackNamePrefix);
        if (launchTemplateUserData != null) {
            launchTemplateUserData.forEach((launchTemplateId, userData) -> {
                if (userData.contains("IS_GATEWAY=false")) {
                    // only gateway instance groups should have proxy settings
                    checkUserDataContains(launchTemplateId, userData, "IS_PROXY_ENABLED", "false");
                } else {
                    ProxyRequest proxyRequest = proxy.getRequest();
                    checkUserDataContains(launchTemplateId, userData, "IS_PROXY_ENABLED", "true");
                    checkUserDataContains(launchTemplateId, userData, "PROXY_HOST", proxyRequest.getHost());
                    checkUserDataContains(launchTemplateId, userData, "PROXY_PORT", proxyRequest.getPort().toString());
                    checkUserDataContains(launchTemplateId, userData, "PROXY_PROTOCOL", proxyRequest.getProtocol());
                    checkUserDataContainsQuoted(launchTemplateId, userData, "PROXY_USER", proxyRequest.getUserName());
                    checkUserDataContainsQuoted(launchTemplateId, userData, "PROXY_PASSWORD", proxyRequest.getPassword());
                    checkUserDataContainsQuoted(launchTemplateId, userData, "PROXY_NO_PROXY_HOSTS", proxyRequest.getNoProxyHosts());
                }
            });
        }
    }

    public Assertion<FreeIpaTestDto, FreeIpaClient> validateFreeIpaUserDataNoProxySettings() {
        return (testContext, testDto, client) -> {
            String stackNamePrefix = getFreeipaStackNamePrefix(testContext);
            validateUserDataNoProxySettings(testContext, stackNamePrefix);
            return testDto;
        };
    }

    public Assertion<SdxInternalTestDto, SdxClient> validateDatalakeUserDataNoProxySettings() {
        return (testContext, testDto, client) -> {
            validateUserDataNoProxySettings(testContext, testDto.getName());
            return testDto;
        };
    }

    public Assertion<DistroXTestDto, CloudbreakClient> validateDatahubUserDataNoProxySettings() {
        return (testContext, testDto, client) -> {
            validateUserDataNoProxySettings(testContext, testDto.getName());
            return testDto;
        };
    }

    private void validateUserDataNoProxySettings(TestContext testContext, String stackNamePrefix) {
        CloudFunctionality cloudFunctionality = testContext.getCloudProvider().getCloudFunctionality();
        Map<String, String> launchTemplateUserData = cloudFunctionality.getLaunchTemplateUserData(stackNamePrefix);
        if (launchTemplateUserData != null) {
            launchTemplateUserData.forEach((launchTemplateId, userData) -> {
                if (userData.contains("IS_GATEWAY=false")) {
                    // only gateway instance groups should have proxy settings
                    checkUserDataContains(launchTemplateId, userData, "IS_PROXY_ENABLED", "false");
                } else {
                    checkUserDataContains(launchTemplateId, userData, "IS_PROXY_ENABLED", "false");
                    checkUserDataDoesNotContain(launchTemplateId, userData, "PROXY_HOST");
                    checkUserDataDoesNotContain(launchTemplateId, userData, "PROXY_PORT");
                    checkUserDataDoesNotContain(launchTemplateId, userData, "PROXY_PROTOCOL");
                    checkUserDataDoesNotContain(launchTemplateId, userData, "PROXY_USER");
                    checkUserDataDoesNotContain(launchTemplateId, userData, "PROXY_PASSWORD");
                    checkUserDataDoesNotContain(launchTemplateId, userData, "PROXY_NO_PROXY_HOSTS");
                }
            });
        }
    }

    private void checkUserDataContains(String launchTemplateId, String userData, String exportKey, String exportValue) {
        String export = String.format(EXPORT_FORMAT, exportKey, exportValue);
        checkUserDataContains(launchTemplateId, userData, export);
    }

    private void checkUserDataContainsQuoted(String launchTemplateId, String userData, String exportKey, String exportValue) {
        String export = String.format(EXPORT_QUOTED_FORMAT, exportKey, exportValue);
        checkUserDataContains(launchTemplateId, userData, export);
    }

    private static void checkUserDataContains(String launchTemplateId, String userData, String export) {
        if (!userData.contains(export)) {
            String sanitizedExport = export.contains("PASSWORD") ? "<password>" : export;
            throw new TestFailException(String.format("Userdata for launch template %s does not contain %s", launchTemplateId, sanitizedExport));
        }
    }

    private void checkUserDataDoesNotContain(String launchTemplateId, String userData, String exportKey) {
        String export = String.format(EXPORT_FORMAT, exportKey, null);
        if (userData.contains(exportKey)) {
            throw new TestFailException(String.format("Userdata for launch template %s must not contain %s", launchTemplateId, export));
        }
    }

    private static String getFreeipaStackNamePrefix(TestContext testContext) {
        return testContext.given(EnvironmentTestDto.class).getName() + "-freeipa";
    }
}
