package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.blueprint.BlueprintCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.blueprint.BlueprintCreateInternalAction;
import com.sequenceiq.it.cloudbreak.action.v4.blueprint.BlueprintDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v4.blueprint.BlueprintGetAction;
import com.sequenceiq.it.cloudbreak.action.v4.blueprint.BlueprintListAction;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;

@Service
public class BlueprintTestClient {

    public Action<BlueprintTestDto, CloudbreakClient> createV4() {
        return new BlueprintCreateAction();
    }

    public Action<BlueprintTestDto, CloudbreakClient> createInternalV4() {
        return new BlueprintCreateInternalAction();
    }

    public Action<BlueprintTestDto, CloudbreakClient> getV4() {
        return new BlueprintGetAction();
    }

    public Action<BlueprintTestDto, CloudbreakClient> listV4() {
        return new BlueprintListAction();
    }

    public Action<BlueprintTestDto, CloudbreakClient> deleteV4() {
        return new BlueprintDeleteAction();
    }
}