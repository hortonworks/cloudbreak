package com.sequenceiq.cloudbreak.structuredevent.repository;

import com.sequenceiq.cloudbreak.service.secret.domain.AccountIdAwareResource;

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
