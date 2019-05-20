package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.info.CloudbreakInfoGetAction;
import com.sequenceiq.it.cloudbreak.dto.info.CloudbreakInfoTestDto;

@Service
public class CloudbreakInfoTestClient {

    public Action<CloudbreakInfoTestDto> get() {
        return new CloudbreakInfoGetAction();
    }

}
