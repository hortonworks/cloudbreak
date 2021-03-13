package com.sequenceiq.freeipa.client.model;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sequenceiq.freeipa.client.deserializer.ListFlatteningDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IpaServer {

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String cn;

    private String dn;

    public String getFqdn() {
        return cn;
    }

    public String getCn() {
        return cn;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", IpaServer.class.getSimpleName() + "[", "]")
                .add("cn='" + cn + "'")
                .add("dn='" + dn + "'")
                .toString();
    }
}
