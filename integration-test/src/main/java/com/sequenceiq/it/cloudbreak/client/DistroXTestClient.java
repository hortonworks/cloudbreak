package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.DistroXBlueprintRequestAction;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.DistroXCreateAction;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.DistroXDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.DistroXGetAction;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.DistroXRefreshAction;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;

@Service
public class DistroXTestClient {

    public Action<DistroXTestDto, CloudbreakClient> create() {
        return new DistroXCreateAction();
    }

    public Action<DistroXTestDto, CloudbreakClient> delete() {
        return new DistroXDeleteAction();
    }

    public Action<DistroXTestDto, CloudbreakClient> blueprintRequest() {
        return new DistroXBlueprintRequestAction();
    }

    public Action<DistroXTestDto, CloudbreakClient> get() {
        return new DistroXGetAction();
    }

    public Action<DistroXTestDto, CloudbreakClient> refresh() {
        return new DistroXRefreshAction();
    }
}
