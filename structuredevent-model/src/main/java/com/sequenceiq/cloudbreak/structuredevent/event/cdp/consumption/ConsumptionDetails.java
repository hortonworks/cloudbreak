package com.sequenceiq.cloudbreak.structuredevent.event.cdp.consumption;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsumptionDetails implements Serializable {

    private Long id;

    private String name;

    private String description;

    private String accountId;

    private String resourceCrn;

    public ConsumptionDetails() {
    }

    public ConsumptionDetails(Long id, String name, String description, String accountId, String resourceCrn) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.accountId = accountId;
        this.resourceCrn = resourceCrn;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
