package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIPACreateAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIPADeleteAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIPADescribeAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIPAAttachChildEnvironmentAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIPADetachChildEnvironmentAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIPARepairAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIPAStartAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIPAStopAction;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPAChildEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPATestDto;

@Service
public class FreeIPATestClient {

    public Action<FreeIPATestDto, FreeIPAClient> create() {
        return new FreeIPACreateAction();
    }

    public Action<FreeIPATestDto, FreeIPAClient> delete() {
        return new FreeIPADeleteAction();
    }

    public Action<FreeIPAChildEnvironmentTestDto, FreeIPAClient> attachChildEnvironment() {
        return new FreeIPAAttachChildEnvironmentAction();
    }

    public Action<FreeIPAChildEnvironmentTestDto, FreeIPAClient> detachChildEnvironment() {
        return new FreeIPADetachChildEnvironmentAction();
    }

    public Action<FreeIPATestDto, FreeIPAClient>  describe() {
        return new FreeIPADescribeAction();
    }

    public Action<FreeIPATestDto, FreeIPAClient> start() {
        return new FreeIPAStartAction();
    }

    public Action<FreeIPATestDto, FreeIPAClient> stop() {
        return new FreeIPAStopAction();
    }

    public Action<FreeIPATestDto, FreeIPAClient> repair(InstanceMetadataType instanceMetadataType) {
        return new FreeIPARepairAction(instanceMetadataType);
    }
}
