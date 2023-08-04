package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v1.terms.TermsPutAction;
import com.sequenceiq.it.cloudbreak.dto.TermsPolicyDto;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

@Service
public class AzureMarketplaceTermsClient {

    public Action<TermsPolicyDto, EnvironmentClient> put() {
        return new TermsPutAction();
    }
}
