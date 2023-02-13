package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.proxy.ProxyConfigCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.proxy.ProxyConfigCreateIfNotExistsAction;
import com.sequenceiq.it.cloudbreak.action.v4.proxy.ProxyConfigDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v4.proxy.ProxyConfigGetAction;
import com.sequenceiq.it.cloudbreak.action.v4.proxy.ProxyConfigListAction;
import com.sequenceiq.it.cloudbreak.dto.proxy.ProxyTestDto;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

@Service
public class ProxyTestClient {

    public Action<ProxyTestDto, EnvironmentClient> create() {
        return new ProxyConfigCreateAction();
    }

    public Action<ProxyTestDto, EnvironmentClient> createIfNotExist() {
        return new ProxyConfigCreateIfNotExistsAction();
    }

    public Action<ProxyTestDto, EnvironmentClient> delete() {
        return new ProxyConfigDeleteAction();
    }

    public Action<ProxyTestDto, EnvironmentClient> get() {
        return new ProxyConfigGetAction();
    }

    public Action<ProxyTestDto, EnvironmentClient> list() {
        return new ProxyConfigListAction();
    }

}