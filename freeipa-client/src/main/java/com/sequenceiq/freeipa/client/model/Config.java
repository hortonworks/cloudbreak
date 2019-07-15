package com.sequenceiq.freeipa.client.model;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sequenceiq.freeipa.client.deserializer.ListFlatteningDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Config {

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private Integer ipamaxusernamelength;

    public Integer getIpamaxusernamelength() {
        return ipamaxusernamelength;
    }

    public void setIpamaxusernamelength(Integer ipamaxusernamelength) {
        this.ipamaxusernamelength = ipamaxusernamelength;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Config.class.getSimpleName() + "[", "]")
                .add("ipamaxusernamelength=" + ipamaxusernamelength)
                .toString();
    }
}
