package com.sequenceiq.it.cloudbreak.dto;

import com.sequenceiq.environment.api.v1.marketplace.model.AzureMarketplaceTermsRequest;
import com.sequenceiq.environment.api.v1.marketplace.model.AzureMarketplaceTermsResponse;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;

@Prototype
public class TermsPolicyDto extends AbstractEnvironmentTestDto<AzureMarketplaceTermsRequest, AzureMarketplaceTermsResponse, TermsPolicyDto> {

    public TermsPolicyDto(TestContext testContext) {
        super(new AzureMarketplaceTermsRequest(), testContext);
    }

    public TermsPolicyDto withAccepted(Boolean accepted) {
        getRequest().setAccepted(accepted);
        return this;
    }

    public TermsPolicyDto valid() {
        return withAccepted(Boolean.TRUE);
    }
}
