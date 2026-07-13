package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.remoteenvironment.RemoteEnvironmentDescribeAction;
import com.sequenceiq.it.cloudbreak.action.remoteenvironment.RemoteEnvironmentListAction;
import com.sequenceiq.it.cloudbreak.dto.remoteenvironment.DescribeRemoteEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.remoteenvironment.ListRemoteEnvironmentsTestDto;
import com.sequenceiq.it.cloudbreak.microservice.RemoteEnvironmentClient;

@Service
public class RemoteEnvironmentTestClient {

    public Action<ListRemoteEnvironmentsTestDto, RemoteEnvironmentClient> list() {
        return new RemoteEnvironmentListAction();
    }

    public Action<DescribeRemoteEnvironmentTestDto, RemoteEnvironmentClient> describe() {
        return new RemoteEnvironmentDescribeAction();
    }
}
