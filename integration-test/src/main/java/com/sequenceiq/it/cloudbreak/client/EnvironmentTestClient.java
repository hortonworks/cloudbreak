package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.EnvironmentServiceClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentGetAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentListAction;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Service
public class EnvironmentTestClient {

    public Action<EnvironmentTestDto, EnvironmentServiceClient> create() {
        return new EnvironmentCreateAction();
    }

    public Action<EnvironmentTestDto, EnvironmentServiceClient> get() {
        return new EnvironmentGetAction();
    }

    public Action<EnvironmentTestDto, EnvironmentServiceClient> list() {
        return new EnvironmentListAction();
    }

    public Action<EnvironmentTestDto, EnvironmentServiceClient> delete() {
        return new EnvironmentDeleteAction();
    }
}
