package com.sequenceiq.cloudbreak.cloud.model.logging;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonLoggingAttributes implements Serializable {

    private final String user;

    private final String group;

    public CommonLoggingAttributes(@JsonProperty("user") String user,
            @JsonProperty("group") String group) {
        this.user = user;
        this.group = group;
    }

    public String getUser() {
        return user;
    }

    public String getGroup() {
        return group;
    }
}
