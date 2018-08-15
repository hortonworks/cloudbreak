package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v3.SmartSenseSubscriptionV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.SmartSenseSubscriptionJson;

@Component
@Transactional(TxType.NEVER)
public class SmartSenseSubscriptionV3Controller implements SmartSenseSubscriptionV3Endpoint {

    @Override
    public Set<SmartSenseSubscriptionJson> listByOrganization(Long organizationId) {
        return null;
    }

    @Override
    public SmartSenseSubscriptionJson getByNameInOrganization(Long organizationId, String name) {
        return null;
    }

    @Override
    public SmartSenseSubscriptionJson getDefaultInOrganization(Long organizationId) {
        return null;
    }

    @Override
    public SmartSenseSubscriptionJson createInOrganization(Long organizationId, SmartSenseSubscriptionJson request) {
        return null;
    }

    @Override
    public SmartSenseSubscriptionJson deleteInOrganization(Long organizationId, String name) {
        return null;
    }
}
