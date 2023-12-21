package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.sdx.saas.SdxSaasCreateAction;
import com.sequenceiq.it.cloudbreak.action.sdx.saas.SdxSaasDeleteAction;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxSaasTestDto;
import com.sequenceiq.it.cloudbreak.microservice.SdxSaasItClient;

@Service
public class SdxSaasTestClient {

    public Action<SdxSaasTestDto, SdxSaasItClient> create() {
        return new SdxSaasCreateAction();
    }

    public Action<SdxSaasTestDto, SdxSaasItClient> delete() {
        return new SdxSaasDeleteAction();
    }

}
