package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentDeleteByNameAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentGetAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentListAction;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Service
public class EnvironmentTestClient {

    public Action<EnvironmentTestDto, EnvironmentClient> create() {
        return new EnvironmentCreateAction();
    }

    public Action<EnvironmentTestDto, EnvironmentClient> describe() {
        return new EnvironmentGetAction();
    }

    public Action<EnvironmentTestDto, EnvironmentClient> list() {
        return new EnvironmentListAction();
    }

    public Action<EnvironmentTestDto, EnvironmentClient> delete() {
        return new EnvironmentDeleteAction();
    }

    public Action<EnvironmentTestDto, EnvironmentClient> deleteByName() {
        return new EnvironmentDeleteByNameAction();
    }
}
