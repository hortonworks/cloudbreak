package com.sequenceiq.freeipa.client.model;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Keytab {

    private String keytab;

    public String getKeytab() {
        return keytab;
    }

    public void setKeytab(String keytab) {
        this.keytab = keytab;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Keytab.class.getSimpleName() + "[", "]")
                .add("keytab='****'")
                .toString();
    }
}
