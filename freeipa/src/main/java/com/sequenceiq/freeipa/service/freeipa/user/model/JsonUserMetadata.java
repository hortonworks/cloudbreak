package com.sequenceiq.freeipa.service.freeipa.user.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class corresponds to the JSON string stored in the 'title' attribute of users and machine users sync'd
 * to the FreeIPA. It contains metadata for the user, including the CRN and workload credentials version.
 * The latter is represented as a Base64 encoding of the binary WorkloadUserSyncActorMetadata protobuf message
 * encoding (see usermanagement.proto).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonUserMetadata {
    private final String crn;

    private final String meta;

    @JsonCreator
    public JsonUserMetadata(
            @JsonProperty(value = "crn", required = true) String crn,
            @JsonProperty(value = "meta", required = true) String meta) {
        this.crn = crn;
        this.meta = meta;
    }

    public String getCrn() {
        return crn;
    }

    public String getMeta() {
        return meta;
    }

    @Override
    public String toString() {
        return "JsonUserMetadata{"
                + "crn='" + crn + '\''
                + ", meta='" + meta + '\''
                + '}';
    }
}
