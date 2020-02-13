package com.sequenceiq.environment.api.v1.tags.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.v1.tags.model.AccountTagBase;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountTagResponse extends AccountTagBase {

    private String accountId;

    private String resourceCrn;

    public AccountTagResponse() {
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

}
