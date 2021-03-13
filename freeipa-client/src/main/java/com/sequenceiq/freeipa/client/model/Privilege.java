package com.sequenceiq.freeipa.client.model;

import java.util.List;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sequenceiq.freeipa.client.deserializer.ListFlatteningDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Privilege {

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String cn;

    @JsonProperty("member_role")
    private List<String> member = List.of();

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

    @Override
    public String toString() {
        return new StringJoiner(", ", Privilege.class.getSimpleName() + "[", "]")
                .add("cn='" + cn + "'")
                .add("member=" + member)
                .toString();
    }
}
