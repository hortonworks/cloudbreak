package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.BlueprintV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.BlueprintRequest;
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;

@Controller
@Transactional(TxType.NEVER)
public class BlueprintV3Controller implements BlueprintV3Endpoint {

    @Override
    public Set<BlueprintResponse> listByOrganization(Long organizationId) {
        return null;
    }

    @Override
    public BlueprintResponse getByNameInOrganization(Long organizationId, String name) {
        return null;
    }

    @Override
    public BlueprintResponse createInOrganization(Long organizationId, BlueprintRequest request) {
        return null;
    }

    @Override
    public BlueprintResponse deleteInOrganization(Long organizationId, String name) {
        return null;
    }
}
