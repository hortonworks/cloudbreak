package com.sequenceiq.cloudbreak.common.dal.model;

public interface AccountAwareResource extends AccountIdAwareResource {

    Long getId();

    String getResourceCrn();

    String getAccountId();

    String getName();

    void setAccountId(String accountId);

    default String getResourceName() {
        return getClass().getSimpleName();
    }
}
