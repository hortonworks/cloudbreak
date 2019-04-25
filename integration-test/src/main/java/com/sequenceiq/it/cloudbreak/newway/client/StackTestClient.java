package com.sequenceiq.it.cloudbreak.newway.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.v4.stack.ChangeImageAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.stack.StackBlueprintRequestAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.stack.StackCreateAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.stack.StackDeleteAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.stack.StackDeleteInstanceAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.stack.StackGetAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.stack.StackModifyAmbariPasswordAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.stack.StackNodeUnhealthyAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.stack.StackRefreshAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.stack.StackRequestAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.stack.StackScalePostAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.stack.StackStartAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.stack.StackStopAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.stack.StackSyncAction;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;

@Service
public class StackTestClient {

    public Action<StackTestDto> nodeUnhealthyV4(String hostgroup, int nodeCount) {
        return new StackNodeUnhealthyAction(hostgroup, nodeCount);
    }

    public StackScalePostAction scalePostV4() {
        return new StackScalePostAction();
    }

    public Action<StackTestDto> createV4() {
        return new StackCreateAction();
    }

    public Action<StackTestDto> deleteV4() {
        return new StackDeleteAction();
    }

    public Action<StackTestDto> blueprintRequestV4() {
        return new StackBlueprintRequestAction();
    }

    public Action<StackTestDto> getV4() {
        return new StackGetAction();
    }

    public Action<StackTestDto> modifyAmbariPasswordV4() {
        return new StackModifyAmbariPasswordAction();
    }

    public Action<StackTestDto> refreshV4() {
        return new StackRefreshAction();
    }

    public Action<StackTestDto> requestV4() {
        return new StackRequestAction();
    }

    public Action<StackTestDto> startV4() {
        return new StackStartAction();
    }

    public Action<StackTestDto> stopV4() {
        return new StackStopAction();
    }

    public Action<StackTestDto> syncV4() {
        return new StackSyncAction();
    }

    public Action<StackTestDto> deleteInstanceV4() {
        return new StackDeleteInstanceAction();
    }

    public Action<StackTestDto> changeImage() {
        return new ChangeImageAction();
    }

}
