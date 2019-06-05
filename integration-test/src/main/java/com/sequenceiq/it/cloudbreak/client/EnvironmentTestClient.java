package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentGetAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentListAction;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Service
public class EnvironmentTestClient {

    public Action<EnvironmentTestDto> createV4() {
        return new EnvironmentCreateAction();
    }

    public Action<EnvironmentTestDto> getV4() {
        return new EnvironmentGetAction();
    }

    public Action<EnvironmentTestDto> listV4() {
        return new EnvironmentListAction();
    }

    public Action<EnvironmentTestDto> deleteV4() {
        return new EnvironmentDeleteAction();
    }
}
