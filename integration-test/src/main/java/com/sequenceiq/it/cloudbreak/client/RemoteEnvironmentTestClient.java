package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.remoteenvironment.RemoteEnvironmentListAction;
import com.sequenceiq.it.cloudbreak.dto.remoteenvironment.RemoteEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.microservice.RemoteEnvironmentClient;

@Service
public class RemoteEnvironmentTestClient {

    public Action<RemoteEnvironmentTestDto, RemoteEnvironmentClient> list() {
        return new RemoteEnvironmentListAction();
    }
}
