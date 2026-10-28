package com.sequenceiq.environment.credential.attributes.gcp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Credentials stored before CB-35227 still carry a p12 node in their attributes JSON.
@JsonIgnoreProperties(ignoreUnknown = true)
public class GcpCredentialAttributes {

    private JsonAttributes json;

    public JsonAttributes getJson() {
        return json;
    }

    public void setJson(JsonAttributes json) {
        this.json = json;
    }
}
