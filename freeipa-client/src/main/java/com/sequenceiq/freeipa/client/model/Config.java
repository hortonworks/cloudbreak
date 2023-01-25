package com.sequenceiq.freeipa.client.model;

import java.util.Set;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sequenceiq.freeipa.client.deserializer.ListFlatteningDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Config {

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private Integer ipamaxusernamelength;

    @JsonDeserialize
    private Set<String> ipauserobjectclasses;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private Integer ipamaxhostnamelength;

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

    public Integer getIpamaxhostnamelength() {
        return ipamaxhostnamelength;
    }

    public void setIpamaxhostnamelength(Integer ipamaxhostnamelength) {
        this.ipamaxhostnamelength = ipamaxhostnamelength;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Config.class.getSimpleName() + "[", "]")
                .add("ipamaxusernamelength=" + ipamaxusernamelength)
                .add("ipauserobjectclasses=" + ipauserobjectclasses)
                .add("ipamaxhostnamelength=" + ipamaxhostnamelength)
                .toString();
    }
}
