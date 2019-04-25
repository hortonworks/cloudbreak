package com.sequenceiq.it.cloudbreak.newway.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.v4.blueprint.BlueprintCreateAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.blueprint.BlueprintDeleteAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.blueprint.BlueprintGetAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.blueprint.BlueprintListAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.blueprint.BlueprintRequestAction;
import com.sequenceiq.it.cloudbreak.newway.dto.blueprint.BlueprintTestDto;

@Service
public class BlueprintTestClient {

    public Action<BlueprintTestDto> createV4() {
        return new BlueprintCreateAction();
    }

    public Action<BlueprintTestDto> getV4() {
        return new BlueprintGetAction();
    }

    public Action<BlueprintTestDto> listV4() {
        return new BlueprintListAction();
    }

    public Action<BlueprintTestDto> deleteV4() {
        return new BlueprintDeleteAction();
    }

    public Action<BlueprintTestDto> requestV4() {
        return new BlueprintRequestAction();
    }
}