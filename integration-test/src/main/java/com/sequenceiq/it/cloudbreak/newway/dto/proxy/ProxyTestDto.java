package com.sequenceiq.it.cloudbreak.newway.dto.proxy;

import javax.ws.rs.WebApplicationException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.requests.ProxyV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.AbstractCloudbreakTestDto;

@Prototype
public class ProxyTestDto extends AbstractCloudbreakTestDto<ProxyV4Request, ProxyV4Response, ProxyTestDto> {
    public static final String PROXY_CONFIG = "PROXY_CONFIG";

    ProxyTestDto(String newId) {
        super(newId);
        setRequest(new ProxyV4Request());
    }

    ProxyTestDto() {
        this(PROXY_CONFIG);
    }

    public ProxyTestDto(TestContext testContext) {
        super(new ProxyV4Request(), testContext);
    }

    public ProxyTestDto(ProxyV4Request proxyV4Request, TestContext testContext) {
        super(proxyV4Request, testContext);
    }

    @Override
    public ProxyTestDto valid() {
        return withName(resourceProperyProvider().getName())
                .withDescription(resourceProperyProvider().getDescription("proxy"))
                .withServerHost("1.2.3.4")
                .withServerUser("mock")
                .withPassword("akarmi")
                .withServerPort(9)
                .withProtocol("http");
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        try {
            cloudbreakClient.getCloudbreakClient().proxyConfigV4Endpoint().delete(cloudbreakClient.getWorkspaceId(), getName());
        } catch (WebApplicationException ignore) {
            LOGGER.info("Something happend.");
        }
    }

    public ProxyTestDto withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
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

    @Override
    public int order() {
        return 500;
    }
}