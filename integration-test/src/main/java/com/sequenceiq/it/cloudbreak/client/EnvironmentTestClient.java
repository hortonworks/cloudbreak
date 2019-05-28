package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentGetAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentListAction;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Service
public class EnvironmentTestClient {

    public Action<EnvironmentTestDto, CloudbreakClient> createV4() {
        return new EnvironmentCreateAction();
    }

    public Action<EnvironmentTestDto, CloudbreakClient> getV4() {
        return new EnvironmentGetAction();
    }

    public Action<EnvironmentTestDto, CloudbreakClient> listV4() {
        return new EnvironmentListAction();
    }

    public Action<EnvironmentTestDto, CloudbreakClient> deleteV4() {
        return new EnvironmentDeleteAction();
    }
}
