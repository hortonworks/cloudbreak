package com.sequenceiq.freeipa.client.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sequenceiq.freeipa.client.deserializer.ListFlatteningDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Role {
    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String cn;

    private List<String> member = new ArrayList<>();

    public String getCn() {
        return cn;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }

    public List<String> getMember() {
        return member;
    }

    public void setMember(List<String> member) {
        this.member = member;
    }
}
