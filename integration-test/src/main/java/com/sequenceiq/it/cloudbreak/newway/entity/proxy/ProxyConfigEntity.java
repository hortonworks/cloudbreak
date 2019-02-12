package com.sequenceiq.it.cloudbreak.newway.entity.proxy;

import javax.ws.rs.WebApplicationException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.requests.ProxyV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Response;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class ProxyConfigEntity extends AbstractCloudbreakEntity<ProxyV4Request, ProxyV4Response, ProxyConfigEntity> {
    public static final String PROXY_CONFIG = "PROXY_CONFIG";

    ProxyConfigEntity(String newId) {
        super(newId);
        setRequest(new ProxyV4Request());
    }

    ProxyConfigEntity() {
        this(PROXY_CONFIG);
    }

    public ProxyConfigEntity(TestContext testContext) {
        super(new ProxyV4Request(), testContext);
    }

    public ProxyConfigEntity(ProxyV4Request proxyV4Request, TestContext testContext) {
        super(proxyV4Request, testContext);
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
            cloudbreakClient.getCloudbreakClient().proxyConfigV4Endpoint().delete(cloudbreakClient.getWorkspaceId(), getName());
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
        getRequest().setHost(serverHost);
        return this;
    }

    public ProxyConfigEntity withServerPort(Integer serverPort) {
        getRequest().setPort(serverPort);
        return this;
    }

    public ProxyConfigEntity withServerUser(String serverUser) {
        getRequest().setUserName(serverUser);
        return this;
    }

    @Override
    public int order() {
        return 500;
    }
}