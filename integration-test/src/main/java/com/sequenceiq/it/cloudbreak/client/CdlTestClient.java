package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.cdl.CdlCreateAction;
import com.sequenceiq.it.cloudbreak.dto.cdl.CdlTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CdlClient;

@Service
public class CdlTestClient {

    public Action<CdlTestDto, CdlClient> create() {
        return new CdlCreateAction();
    }
}
