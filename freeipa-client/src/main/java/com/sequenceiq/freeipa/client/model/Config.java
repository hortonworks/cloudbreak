package com.sequenceiq.freeipa.client.model;

import java.util.Set;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sequenceiq.freeipa.client.deserializer.ListFlatteningDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Config {

    // users for password setting in FreeIPA Plugin
    public static final String CDP_USER_ATTRIBUTE = "cdpUserAttr";

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private Integer ipamaxusernamelength;

    @JsonDeserialize
    private Set<String> ipauserobjectclasses;

    public Integer getIpamaxusernamelength() {
        return ipamaxusernamelength;
    }

    public void setIpamaxusernamelength(Integer ipamaxusernamelength) {
        this.ipamaxusernamelength = ipamaxusernamelength;
    }

    public Set<String> getIpauserobjectclasses() {
        return ipauserobjectclasses;
    }

    public void setIpauserobjectclasses(Set<String> ipauserobjectclasses) {
        this.ipauserobjectclasses = ipauserobjectclasses;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Config.class.getSimpleName() + "[", "]")
                .add("ipamaxusernamelength=" + ipamaxusernamelength)
                .add(String.format("ipauserobjectclasses={}", ipauserobjectclasses != null ? ipauserobjectclasses.toString() : ""))
                .toString();
    }
}
