package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIPACreateAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIPADeleteAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIPADescribeAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIPAStartAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIPAStopAction;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPATestDto;

@Service
public class FreeIPATestClient {

    public Action<FreeIPATestDto, FreeIPAClient> create() {
        return new FreeIPACreateAction();
    }

    public Action<FreeIPATestDto, FreeIPAClient> delete() {
        return new FreeIPADeleteAction();
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
}
