package com.sequenceiq.cloudbreak.controller.v4;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.smartsense.SmartSenseSubscriptionV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.smartsense.base.SmartSenseSubscriptionListV4Filter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.smartsense.responses.SmartSenseSubscriptionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.smartsense.responses.SmartSenseSubscriptionV4Responses;

@Controller
@Transactional(TxType.NEVER)
public class SmartSenseSubscriptionV4Controller implements SmartSenseSubscriptionV4Endpoint {

    @Override
    public SmartSenseSubscriptionV4Responses list(Long workspaceId, SmartSenseSubscriptionListV4Filter filter) {
        return null;
    }

    @Override
    public SmartSenseSubscriptionV4Response get(Long workspaceId, String name) {
        return null;
    }

    @Override
    public SmartSenseSubscriptionV4Response create(Long workspaceId, SmartSenseSubscriptionV4Response request) {
        return null;
    }

    @Override
    public SmartSenseSubscriptionV4Response delete(Long workspaceId, String name) {
        return null;
    }
}
