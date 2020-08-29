package com.sequenceiq.cloudbreak.structuredevent.repository;


public interface AccountAwareResource {

    Long getId();

    String getResourceCrn();

    String getAccountId();

    String getName();

    void setAccountId(String accountId);

    String getCreator();

    default String getResourceName() {
        return getClass().getSimpleName().toLowerCase();
    }
}
