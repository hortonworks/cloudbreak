package com.sequenceiq.it.cloudbreak.newway.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.v4.environment.EnvironmentAttachAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.environment.EnvironmentChangeCredentialAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.environment.EnvironmentCreateAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.environment.EnvironmentDeleteAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.environment.EnvironmentDetachAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.environment.EnvironmentGetAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.environment.EnvironmentListAction;
import com.sequenceiq.it.cloudbreak.newway.dto.environment.EnvironmentTestDto;

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

    public Action<EnvironmentTestDto> attachV4() {
        return new EnvironmentAttachAction();
    }

    public Action<EnvironmentTestDto> detachV4() {
        return new EnvironmentDetachAction();
    }

    public Action<EnvironmentTestDto> deleteV4() {
        return new EnvironmentDeleteAction();
    }

    public Action<EnvironmentTestDto> changeCredential() {
        return new EnvironmentChangeCredentialAction();
    }
}
