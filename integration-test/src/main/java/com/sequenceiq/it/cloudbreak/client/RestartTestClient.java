package com.sequenceiq.it.cloudbreak.client;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.stack.RestartInstancesAction;
import com.sequenceiq.it.cloudbreak.dto.restart.RestartInstancesTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

@Service
public class RestartTestClient {
    public Action<RestartInstancesTestDto, CloudbreakClient> restartInstances(List<String> instanceIds) {
        return new RestartInstancesAction(instanceIds);
    }
}
