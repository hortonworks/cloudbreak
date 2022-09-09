package com.sequenceiq.freeipa.client.model;

import java.util.List;

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

    @JsonProperty("memberof_permission")
    private List<String> memberofPermission = List.of();

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

    public List<String> getMemberofPermission() {
        return memberofPermission;
    }

    public void setMemberofPermission(List<String> memberofPermission) {
        this.memberofPermission = memberofPermission;
    }

    @Override
    public String toString() {
        return "Privilege{" +
                "cn='" + cn + '\'' +
                ", member=" + member +
                ", memberofPermission=" + memberofPermission +
                '}';
    }
}
