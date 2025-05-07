package com.sequenceiq.it.cloudbreak.assertion.proxy;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.environment.api.v1.proxy.model.request.ProxyRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.AbstractTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.proxy.ProxyTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshProxyActions;

@Component
class ProxyConfigFileAssertions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigFileAssertions.class);

    private static final String PROXY_PASSWORD_PATTERN = "(https_proxy=.*//[^:]*:)(.*)(@.*)";

    private static final String PROXY_PASSWORD_REPLACED_VALUE = "$1<password>$3";

    @Inject
    private SshProxyActions sshProxyActions;

    public Assertion<FreeIpaTestDto, FreeIpaClient> validateFreeIpaNoProxyConfigFile() {
        return (testContext, testDto, client) -> validateNoProxyConfigFile(testDto, getFreeipaIpAddresses(testDto));
    }

    public Assertion<SdxInternalTestDto, SdxClient> validateDatalakeNoProxyConfigFile() {
        return (testContext, testDto, client) -> validateNoProxyConfigFile(testDto, getSdxIpAddresses(testDto));
    }

    public Assertion<DistroXTestDto, CloudbreakClient> validateDatahubNoProxyConfigFile() {
        return (testContext, testDto, client) -> validateNoProxyConfigFile(testDto, getDistroXIpAddresses(testDto));
    }

    private <T extends AbstractTestDto<?, ?, ?, ?>> T validateNoProxyConfigFile(T testDto, Set<String> ipAddresses) {
        LOGGER.info("Validating {} has no proxy config file on node(s) {}", testDto.getCrn(), ipAddresses);
        Optional<String> proxySettings = sshProxyActions.getProxySettings(ipAddresses);
        if (proxySettings.isPresent()) {
            throw new TestFailException(String.format("Expected no proxy settings but found %s", sanitizeCcmProxySettings(proxySettings.get())));
        }
        return testDto;
    }

    public Assertion<FreeIpaTestDto, FreeIpaClient> validateFreeIpaProxyConfigFile(ProxyTestDto proxy) {
        return (testContext, testDto, client) -> validateProxyConfigFile(testDto, getFreeipaIpAddresses(testDto), proxy);
    }

    public Assertion<SdxInternalTestDto, SdxClient> validateDatalakeProxyConfigFile(ProxyTestDto proxy) {
        return (testContext, testDto, client) -> validateProxyConfigFile(testDto, getSdxIpAddresses(testDto), proxy);
    }

    public Assertion<DistroXTestDto, CloudbreakClient> validateDatahubProxyConfigFile(ProxyTestDto proxy) {
        return (testContext, testDto, client) -> validateProxyConfigFile(testDto, getDistroXIpAddresses(testDto), proxy);
    }

    private <T extends AbstractTestDto<?, ?, ?, ?>> T validateProxyConfigFile(T testDto, Set<String> ipAddresses, ProxyTestDto proxy) {
        LOGGER.info("Validating {} has {} proxy config file on node(s) {}", testDto.getCrn(), proxy.getName(), ipAddresses);
        String proxySettings = sshProxyActions.getProxySettings(ipAddresses)
                .orElseThrow(() -> new TestFailException("Expected proxy settings but found none"));
        ProxyRequest proxyRequest = proxy.getRequest();
        String expectedProxySettings = String.format("https_proxy=%s://%s:%s@%s:%s\r\nno_proxy=%s\r\n",
                proxyRequest.getProtocol(), proxyRequest.getUserName(), proxyRequest.getPassword(),
                proxyRequest.getHost(), proxyRequest.getPort(), proxyRequest.getNoProxyHosts());
        if (!proxySettings.equals(expectedProxySettings)) {
            throw new TestFailException(String.format("Proxy settings does not match! Expected: '%s', Received: '%s'",
                    sanitizeCcmProxySettings(expectedProxySettings), sanitizeCcmProxySettings(proxySettings)));
        }
        return testDto;
    }

    private static String sanitizeCcmProxySettings(String proxySettings) {
        return proxySettings.replaceAll(PROXY_PASSWORD_PATTERN, PROXY_PASSWORD_REPLACED_VALUE);
    }

    private static Set<String> getFreeipaIpAddresses(FreeIpaTestDto testDto) {
        return testDto.getResponse().getInstanceGroups().stream()
                .flatMap(ig -> ig.getMetaData().stream())
                .map(InstanceMetaDataResponse::getPrivateIp)
                .collect(Collectors.toSet());
    }

    private static Set<String> getSdxIpAddresses(SdxInternalTestDto testDto) {
        return getStackIpAddresses(testDto.getResponse().getStackV4Response());
    }

    private static Set<String> getDistroXIpAddresses(DistroXTestDto testDto) {
        return getStackIpAddresses(testDto.getResponse());
    }

    private static Set<String> getStackIpAddresses(StackV4Response stack) {
        return stack.getInstanceGroups().stream()
                .filter(ig -> ig.getType().equals(InstanceGroupType.GATEWAY))
                .flatMap(ig -> ig.getMetadata().stream())
                .map(InstanceMetaDataV4Response::getPrivateIp)
                .collect(Collectors.toSet());
    }
}
