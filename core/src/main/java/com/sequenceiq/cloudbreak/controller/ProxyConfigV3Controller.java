package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.ProxyConfigV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigRequest;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigResponse;

@Controller
@Transactional(TxType.NEVER)
public class ProxyConfigV3Controller implements ProxyConfigV3Endpoint {

    @Override
    public Set<ProxyConfigResponse> listByOrganization(Long organizationId) {
        return null;
    }

    @Override
    public ProxyConfigResponse getByNameInOrganization(Long organizationId, String name) {
        return null;
    }

    @Override
    public ProxyConfigResponse createInOrganization(Long organizationId, ProxyConfigRequest request) {
        return null;
    }

    @Override
    public ProxyConfigResponse deleteInOrganization(Long organizationId, String name) {
        return null;
    }
}
