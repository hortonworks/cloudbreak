package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.stack.StackAttachRecipeAction;
import com.sequenceiq.it.cloudbreak.action.v4.stack.StackBlueprintRequestAction;
import com.sequenceiq.it.cloudbreak.action.v4.stack.StackCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.stack.StackDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v4.stack.StackDeleteInstanceAction;
import com.sequenceiq.it.cloudbreak.action.v4.stack.StackForceDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v4.stack.StackGetAction;
import com.sequenceiq.it.cloudbreak.action.v4.stack.StackRefreshAction;
import com.sequenceiq.it.cloudbreak.action.v4.stack.StackRequestAction;
import com.sequenceiq.it.cloudbreak.action.v4.stack.StackStartAction;
import com.sequenceiq.it.cloudbreak.action.v4.stack.StackStopAction;
import com.sequenceiq.it.cloudbreak.action.v4.stack.StackSyncAction;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

@Service
public class StackTestClient {

    public Action<StackTestDto, CloudbreakClient> createV4() {
        return new StackCreateAction();
    }

    public Action<StackTestDto, CloudbreakClient> deleteV4() {
        return new StackDeleteAction();
    }

    public Action<StackTestDto, CloudbreakClient> forceDeleteV4() {
        return new StackForceDeleteAction();
    }

    public Action<StackTestDto, CloudbreakClient> blueprintRequestV4() {
        return new StackBlueprintRequestAction();
    }

    public Action<StackTestDto, CloudbreakClient> getV4() {
        return new StackGetAction();
    }

    public Action<StackTestDto, CloudbreakClient> refreshV4() {
        return new StackRefreshAction();
    }

    public Action<StackTestDto, CloudbreakClient> requestV4() {
        return new StackRequestAction();
    }

    public Action<StackTestDto, CloudbreakClient> startV4() {
        return new StackStartAction();
    }

    public Action<StackTestDto, CloudbreakClient> stopV4() {
        return new StackStopAction();
    }

    public Action<StackTestDto, CloudbreakClient> syncV4() {
        return new StackSyncAction();
    }

    public Action<StackTestDto, CloudbreakClient> deleteInstanceV4() {
        return new StackDeleteInstanceAction();
    }

    public Action<StackTestDto, CloudbreakClient> attachRecipeV4() {
        return new StackAttachRecipeAction();
    }
}
