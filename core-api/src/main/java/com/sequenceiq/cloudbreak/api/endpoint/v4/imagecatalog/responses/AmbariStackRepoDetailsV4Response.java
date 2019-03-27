package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AmbariStackRepoDetailsV4Response extends BaseStackRepoDetailsV4Response implements JsonEntity {

    @JsonProperty("util")
    private Map<String, String> util;

    public Map<String, String> getUtil() {
        return util;
    }

    public void setUtil(Map<String, String> util) {
        this.util = util;
    }
}
