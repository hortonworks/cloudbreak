package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.FlexSubscriptionV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.FlexSubscriptionRequest;
import com.sequenceiq.cloudbreak.api.model.FlexSubscriptionResponse;

@Controller
@Transactional(TxType.NEVER)
public class FlexSubscriptionV3Controller implements FlexSubscriptionV3Endpoint {

    @Override
    public Set<FlexSubscriptionResponse> listByOrganization(Long organizationId) {
        return null;
    }

    @Override
    public FlexSubscriptionResponse getByNameInOrganization(Long organizationId, String name) {
        return null;
    }

    @Override
    public FlexSubscriptionResponse createInOrganization(Long organizationId, FlexSubscriptionRequest request) {
        return null;
    }

    @Override
    public FlexSubscriptionResponse deleteInOrganization(Long organizationId, String name) {
        return null;
    }

    @Override
    public void setUsedForControllerInOrganization(Long organizationId, Long name) {

    }

    @Override
    public void setDefaultInOrganization(Long organizationId, Long id) {

    }
}
