package com.sequenceiq.it.cloudbreak.newway;

import javax.ws.rs.WebApplicationException;

import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigRequest;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigResponse;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class ProxyConfigEntity extends AbstractCloudbreakEntity<ProxyConfigRequest, ProxyConfigResponse, ProxyConfigEntity> {
    public static final String PROXY_CONFIG = "PROXY_CONFIG";

    ProxyConfigEntity(String newId) {
        super(newId);
        setRequest(new ProxyConfigRequest());
    }

    ProxyConfigEntity() {
        this(PROXY_CONFIG);
    }

    public ProxyConfigEntity(TestContext testContext) {
        super(new ProxyConfigRequest(), testContext);
    }

    public ProxyConfigEntity(ProxyConfigRequest proxyConfigRequest, TestContext testContext) {
        super(proxyConfigRequest, testContext);
    }

    @Override
    public ProxyConfigEntity valid() {
        return withName(getNameCreator().getRandomNameForMock())
                .withDescription("Proxy config for integration test")
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
            cloudbreakClient.getCloudbreakClient().proxyConfigV3Endpoint().deleteInWorkspace(cloudbreakClient.getWorkspaceId(), getName());
        } catch (WebApplicationException ignore) {
            LOGGER.info("Something happend.");
        }
    }

    public ProxyConfigEntity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public ProxyConfigEntity withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    public ProxyConfigEntity withPassword(String password) {
        getRequest().setPassword(password);
        return this;
    }

    public ProxyConfigEntity withProtocol(String protocol) {
        getRequest().setProtocol(protocol);
        return this;
    }

    public ProxyConfigEntity withServerHost(String serverHost) {
        getRequest().setServerHost(serverHost);
        return this;
    }

    public ProxyConfigEntity withServerPort(Integer serverPort) {
        getRequest().setServerPort(serverPort);
        return this;
    }

    public ProxyConfigEntity withServerUser(String serverUser) {
        getRequest().setUserName(serverUser);
        return this;
    }
}