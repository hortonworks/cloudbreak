package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.stack.StackGetAction;
import com.sequenceiq.it.cloudbreak.action.v4.stack.StackRefreshAction;
import com.sequenceiq.it.cloudbreak.action.v4.stack.StackRefreshEntitlementParamInternalAction;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

@Service
public class StackTestClient {

    public Action<StackTestDto, CloudbreakClient> getV4() {
        return new StackGetAction();
    }

    public Action<StackTestDto, CloudbreakClient> refreshV4() {
        return new StackRefreshAction();
    }

    public Action<StackTestDto, CloudbreakClient> refreshEntitlementParamsV4() {
        return new StackRefreshEntitlementParamInternalAction();
    }
}
