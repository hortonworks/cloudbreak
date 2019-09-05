package com.sequenceiq.freeipa.client.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sequenceiq.freeipa.client.deserializer.ListFlatteningDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Role {
    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String cn;

    @JsonProperty("member_user")
    private List<String> memberUser = new ArrayList<>();

    @JsonProperty("member_group")
    private List<String> memberGroup = new ArrayList<>();

    @JsonProperty("member_host")
    private List<String> memberHost = new ArrayList<>();

    @JsonProperty("member_hostgroup")
    private List<String> memberHostGroup = new ArrayList<>();

    @JsonProperty("member_service")
    private List<String> memberService = new ArrayList<>();

    public String getCn() {
        return cn;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }

    public List<String> getMemberUser() {
        return memberUser;
    }

    public void setMemberUser(List<String> memberUser) {
        this.memberUser = memberUser;
    }

    public List<String> getMemberGroup() {
        return memberGroup;
    }

    public void setMemberGroup(List<String> memberGroup) {
        this.memberGroup = memberGroup;
    }

    public List<String> getMemberHost() {
        return memberHost;
    }

    public void setMemberHost(List<String> memberHost) {
        this.memberHost = memberHost;
    }

    public List<String> getMemberHostGroup() {
        return memberHostGroup;
    }

    public void setMemberHostGroup(List<String> memberHostGroup) {
        this.memberHostGroup = memberHostGroup;
    }

    public List<String> getMemberService() {
        return memberService;
    }

    public void setMemberService(List<String> memberService) {
        this.memberService = memberService;
    }
}
