package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.customconfigs.CustomConfigurationsCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.customconfigs.CustomConfigurationsDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v4.customconfigs.CustomConfigurationsGetAction;
import com.sequenceiq.it.cloudbreak.action.v4.customconfigs.CustomConfigurationsListAction;
import com.sequenceiq.it.cloudbreak.dto.customconfigs.CustomConfigurationsTestDto;

@Service
public class CustomConfigurationsTestClient {

    public Action<CustomConfigurationsTestDto, CloudbreakClient> createV4() {
        return new CustomConfigurationsCreateAction();
    }

    public Action<CustomConfigurationsTestDto, CloudbreakClient> getV4() {
        return new CustomConfigurationsGetAction();
    }

    public Action<CustomConfigurationsTestDto, CloudbreakClient> deleteV4() {
        return new CustomConfigurationsDeleteAction();
    }

    public Action<CustomConfigurationsTestDto, CloudbreakClient> listV4() {
        return new CustomConfigurationsListAction();
    }
}
