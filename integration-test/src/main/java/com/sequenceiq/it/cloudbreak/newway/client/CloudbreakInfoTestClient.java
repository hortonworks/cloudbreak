package com.sequenceiq.it.cloudbreak.newway.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.v4.info.CloudbreakInfoGetAction;
import com.sequenceiq.it.cloudbreak.newway.dto.info.CloudbreakInfoTestDto;

@Service
public class CloudbreakInfoTestClient {

    public Action<CloudbreakInfoTestDto> get() {
        return new CloudbreakInfoGetAction();
    }

}
