package com.sequenceiq.common.api.cloudstorage;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.api.cloudstorage.doc.CloudStorageModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountMappingBase implements Serializable {

    @ApiModelProperty(CloudStorageModelDescription.GROUP_MAPPINGS)
    private Map<String, String> groupMappings = new HashMap<>();

    @ApiModelProperty(CloudStorageModelDescription.USER_MAPPINGS)
    private Map<String, String> userMappings = new HashMap<>();

    public Map<String, String> getGroupMappings() {
        return groupMappings;
    }

    public void setGroupMappings(Map<String, String> groupMappings) {
        this.groupMappings = groupMappings == null ? new HashMap<>() : groupMappings;
    }

    public Map<String, String> getUserMappings() {
        return userMappings;
    }

    public void setUserMappings(Map<String, String> userMappings) {
        this.userMappings = userMappings == null ? new HashMap<>() : userMappings;
    }

}
