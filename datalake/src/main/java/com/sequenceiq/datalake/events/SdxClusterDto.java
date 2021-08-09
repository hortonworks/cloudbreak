package com.sequenceiq.datalake.events;

import com.sequenceiq.cloudbreak.structuredevent.repository.AccountAwareResource;

public class SdxClusterDto implements AccountAwareResource {

    private Long id;

    private String name;

    private String accountId;

    private String resourceCrn;

    private String resourceName;

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String getResourceCrn() {
        return resourceCrn;
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public String getResourceName() {
        return resourceName;
    }
}
