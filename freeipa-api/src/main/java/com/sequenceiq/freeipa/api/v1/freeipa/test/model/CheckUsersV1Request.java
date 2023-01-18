package com.sequenceiq.freeipa.api.v1.freeipa.test.model;

import java.util.Set;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CheckUsersV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckUsersV1Request extends ClientTestBaseRequest {

    @NotEmpty
    @Schema(description = ModelDescriptions.USERS, required = true)
    private Set<String> users;

    public Set<String> getUsers() {
        return users;
    }

    public void setUsers(Set<String> users) {
        this.users = users;
    }
}
