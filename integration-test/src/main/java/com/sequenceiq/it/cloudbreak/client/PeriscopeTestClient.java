package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.AutoscaleConfigAction;
import com.sequenceiq.it.cloudbreak.dto.autoscale.AutoScaleConfigDto;
import com.sequenceiq.it.cloudbreak.microservice.PeriscopeClient;

@Service
public class PeriscopeTestClient {

    public Action<AutoScaleConfigDto, PeriscopeClient> create() {
        return new AutoscaleConfigAction();
    }

}
