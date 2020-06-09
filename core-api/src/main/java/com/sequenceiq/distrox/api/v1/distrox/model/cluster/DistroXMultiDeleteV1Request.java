package com.sequenceiq.distrox.api.v1.distrox.model.cluster;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.authorization.annotation.ResourceObjectField;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationVariableType;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DistroXMultiDeleteV1Request {

    @ResourceObjectField(action = AuthorizationResourceAction.DELETE_DATAHUB, variableType = AuthorizationVariableType.NAME_LIST)
    private Set<String> names = new HashSet<>();

    @ResourceObjectField(action = AuthorizationResourceAction.DELETE_DATAHUB, variableType = AuthorizationVariableType.CRN_LIST)
    private Set<String> crns = new HashSet<>();

    public Set<String> getNames() {
        return names;
    }

    public void setNames(Set<String> names) {
        if (names != null) {
            this.names = names;
        }
    }

    public Set<String> getCrns() {
        return crns;
    }

    public void setCrns(Set<String> crns) {
        if (crns != null) {
            this.crns = crns;
        }
    }
}
