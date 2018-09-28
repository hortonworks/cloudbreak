package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.SmartSenseSubscriptionV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.SmartSenseSubscriptionJson;

@Controller
@Transactional(TxType.NEVER)
public class SmartSenseSubscriptionV3Controller implements SmartSenseSubscriptionV3Endpoint {

    @Override
    public Set<SmartSenseSubscriptionJson> listByWorkspace(Long workspaceId) {
        return null;
    }

    @Override
    public SmartSenseSubscriptionJson getByNameInWorkspace(Long workspaceId, String name) {
        return null;
    }

    @Override
    public SmartSenseSubscriptionJson getDefaultInWorkspace(Long workspaceId) {
        return null;
    }

    @Override
    public SmartSenseSubscriptionJson createInWorkspace(Long workspaceId, SmartSenseSubscriptionJson request) {
        return null;
    }

    @Override
    public SmartSenseSubscriptionJson deleteInWorkspace(Long workspaceId, String name) {
        return null;
    }
}
