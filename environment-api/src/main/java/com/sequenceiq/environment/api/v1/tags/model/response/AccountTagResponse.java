package com.sequenceiq.environment.api.v1.tags.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.v1.tags.model.AccountTagBase;
import com.sequenceiq.environment.api.v1.tags.model.AccountTagStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountTagResponse extends AccountTagBase {

    private String accountId;

    private String resourceCrn;

    private AccountTagStatus status = AccountTagStatus.DEFAULT;

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

    public AccountTagStatus getStatus() {
        return status;
    }

    public void setStatus(AccountTagStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return super.toString() + ", " + "AccountTagResponse{" +
                "accountId='" + accountId + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", status=" + status +
                '}';
    }
}
