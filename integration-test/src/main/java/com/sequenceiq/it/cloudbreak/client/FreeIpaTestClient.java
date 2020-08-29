package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaCollectDiagnosticsAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaCreateAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaDeleteAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaDescribeAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaAttachChildEnvironmentAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaDetachChildEnvironmentAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaRepairAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaStartAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaStopAction;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaChildEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaDiagnosticsTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;

@Service
public class FreeIpaTestClient {

    public Action<FreeIpaTestDto, FreeIpaClient> create() {
        return new FreeIpaCreateAction();
    }

    public Action<FreeIpaTestDto, FreeIpaClient> delete() {
        return new FreeIpaDeleteAction();
    }

    public Action<FreeIpaChildEnvironmentTestDto, FreeIpaClient> attachChildEnvironment() {
        return new FreeIpaAttachChildEnvironmentAction();
    }

    public Action<FreeIpaChildEnvironmentTestDto, FreeIpaClient> detachChildEnvironment() {
        return new FreeIpaDetachChildEnvironmentAction();
    }

    public Action<FreeIpaTestDto, FreeIpaClient>  describe() {
        return new FreeIpaDescribeAction();
    }

    public Action<FreeIpaTestDto, FreeIpaClient> start() {
        return new FreeIpaStartAction();
    }

    public Action<FreeIpaTestDto, FreeIpaClient> stop() {
        return new FreeIpaStopAction();
    }

    public Action<FreeIpaTestDto, FreeIpaClient> repair(InstanceMetadataType instanceMetadataType) {
        return new FreeIpaRepairAction(instanceMetadataType);
    }

    public Action<FreeIpaDiagnosticsTestDto, FreeIpaClient> collectDiagnostics() {
        return new FreeIpaCollectDiagnosticsAction();
    }
}
