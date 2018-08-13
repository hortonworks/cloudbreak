package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v3.StackV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;

@Component
@Transactional(TxType.NEVER)
public class StackV3Controller extends NotificationController implements StackV3Endpoint {

    @Override
    public Set<StackResponse> listByOrganization(Long organizationId) {
        return null;
    }

    @Override
    public StackResponse getByNameInOrganization(Long organizationId, String name) {
        return null;
    }

    @Override
    public StackResponse createInOrganization(Long organizationId, StackV2Request request) {
        return null;
    }

    @Override
    public StackResponse deleteInOrganization(Long organizationId, String name) {
        return null;
    }
}
