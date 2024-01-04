package com.sequenceiq.authorization.info.model;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@NotNull
public class CheckRightV4Request {

    private List<RightV4> rights;

    public List<RightV4> getRights() {
        return rights;
    }

    public void setRights(List<RightV4> rights) {
        this.rights = rights;
    }
}
