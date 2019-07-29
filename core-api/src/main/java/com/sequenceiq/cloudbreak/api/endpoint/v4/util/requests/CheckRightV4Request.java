package com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.base.RightV4;

import io.swagger.annotations.ApiModel;

@ApiModel
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
