package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigRequest;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigResponse;

public class ProxyConfigEntity extends AbstractCloudbreakEntity<ProxyConfigRequest, ProxyConfigResponse, ProxyConfigEntity> {
    public static final String PROXY_CONFIG = "PROXY_CONFIG";

    ProxyConfigEntity(String newId) {
        super(newId);
        setRequest(new ProxyConfigRequest());
    }

    ProxyConfigEntity() {
        this(PROXY_CONFIG);
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