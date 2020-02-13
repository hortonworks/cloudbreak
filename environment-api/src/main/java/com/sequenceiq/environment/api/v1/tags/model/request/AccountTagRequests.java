package com.sequenceiq.environment.api.v1.tags.model.request;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountTagRequests {

    @NotNull
    private Set<AccountTagRequest> tags = new HashSet<>();

    public Set<AccountTagRequest> getTags() {
        return tags;
    }

    public void setTags(Set<AccountTagRequest> tags) {
        this.tags = tags;
    }
}
