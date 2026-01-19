package com.sequenceiq.it.cloudbreak.dto.proxy;

import jakarta.inject.Inject;

import com.sequenceiq.environment.api.v1.proxy.endpoint.ProxyEndpoint;
import com.sequenceiq.environment.api.v1.proxy.model.request.ProxyRequest;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponse;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.config.ProxyConfigProperties;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractEnvironmentTestDto;

@Prototype
public class ProxyTestDto extends AbstractEnvironmentTestDto<ProxyRequest, ProxyResponse, ProxyTestDto> {

    private static final String PROXYCONFIG_RESOURCE_NAME = "proxyName";

    @Inject
    private ProxyConfigProperties proxyConfigProperties;

    public ProxyTestDto(TestContext testContext) {
        super(new ProxyRequest(), testContext);
    }

    public ProxyTestDto(ProxyRequest proxyV4Request, TestContext testContext) {
        super(proxyV4Request, testContext);
    }

    @Override
    public ProxyTestDto valid() {
        return withName(getResourcePropertyProvider().getName(getCloudPlatform()))
                .withDescription(getResourcePropertyProvider().getDescription("proxy"))
                .withProtocol(proxyConfigProperties.getProxyProtocol())
                .withServerHost(proxyConfigProperties.getProxyHost())
                .withServerPort(proxyConfigProperties.getProxyPort())
                .withServerUser(proxyConfigProperties.getProxyUser())
                .withPassword(proxyConfigProperties.getProxyPassword())
                .withNoProxyHosts(proxyConfigProperties.getProxyNoProxyHosts());
    }

    public ProxyTestDto withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public ProxyTestDto withGeneratedName() {
        String name = getResourcePropertyProvider().getName(getCloudPlatform());
        getRequest().setName(name);
        setName(name);
        return this;
    }

    @Override
    public String getResourceNameType() {
        return PROXYCONFIG_RESOURCE_NAME;
    }

    public ProxyTestDto withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    public ProxyTestDto withPassword(String password) {
        getRequest().setPassword(password);
        return this;
    }

    public ProxyTestDto withProtocol(String protocol) {
        getRequest().setProtocol(protocol);
        return this;
    }

    public ProxyTestDto withServerHost(String serverHost) {
        getRequest().setHost(serverHost);
        return this;
    }

    public ProxyTestDto withServerPort(Integer serverPort) {
        getRequest().setPort(serverPort);
        return this;
    }

    public ProxyTestDto withServerUser(String serverUser) {
        getRequest().setUserName(serverUser);
        return this;
    }

    public ProxyTestDto withNoProxyHosts(String noProxyHosts) {
        getRequest().setNoProxyHosts(noProxyHosts);
        return this;
    }

    @Override
    public String getCrn() {
        return getResponse().getCrn();
    }

    @Override
    public int order() {
        return 500;
    }

    @Override
    public void deleteForCleanup() {
        ProxyEndpoint proxyEndpoint = getClientForCleanup().getDefaultClient(getTestContext()).proxyV1Endpoint();
        proxyEndpoint.deleteByCrn(getCrn());
    }
}
