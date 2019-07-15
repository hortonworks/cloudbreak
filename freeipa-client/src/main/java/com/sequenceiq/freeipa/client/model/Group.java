package com.sequenceiq.freeipa.client.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sequenceiq.freeipa.client.deserializer.ListFlatteningDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Group {
    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String cn;

    private List<String> memberUser;

    public String getCn() {
        return cn;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }

    @JsonProperty("member_user")
    public List<String> getMemberUser() {
        return memberUser;
    }

    @JsonProperty("member_user")
    public void setMemberUser(List<String> memberUser) {
        this.memberUser = memberUser;
    }

    @Override
    public String toString() {
        return "Group{"
                + "cn='" + cn + '\''
                + "memberUser='" + memberUser + '\''
                + '}';
    }
}
