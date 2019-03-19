package com.sequenceiq.it.cloudbreak.newway.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.v4.proxy.ProxyConfigCreateAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.proxy.ProxyConfigCreateIfNotExistsAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.proxy.ProxyConfigDeleteAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.proxy.ProxyConfigGetAction;
import com.sequenceiq.it.cloudbreak.newway.dto.proxy.ProxyTestDto;

@Service
public class ProxyTestClient {

    public Action<ProxyTestDto> createV4() {
        return new ProxyConfigCreateAction();
    }

    public Action<ProxyTestDto> createIfNotExistV4() {
        return new ProxyConfigCreateIfNotExistsAction();
    }

    public Action<ProxyTestDto> deleteV4() {
        return new ProxyConfigDeleteAction();
    }

    public Action<ProxyTestDto> getV4() {
        return new ProxyConfigGetAction();
    }

}