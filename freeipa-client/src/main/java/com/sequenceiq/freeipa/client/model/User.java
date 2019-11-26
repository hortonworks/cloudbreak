package com.sequenceiq.freeipa.client.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sequenceiq.freeipa.client.deserializer.ListFlatteningDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    private String dn;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String sn;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String uid;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String givenname;

    @JsonProperty(value = "memberof_group")
    private List<String> memberOfGroup;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String krbPasswordExpiration;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getGivenname() {
        return givenname;
    }

    public void setGivenname(String givenname) {
        this.givenname = givenname;
    }

    public List<String> getMemberOfGroup() {
        return memberOfGroup;
    }

    public void setMemberOfGroup(List<String> memberOfGroup) {
        this.memberOfGroup = memberOfGroup;
    }

    public String getKrbPasswordExpiration() {
        return krbPasswordExpiration;
    }

    public void setKrbPasswordExpiration(String krbPasswordExpiration) {
        this.krbPasswordExpiration = krbPasswordExpiration;
    }

    @Override
    public String toString() {
        return "User{"
                + "dn='" + dn + '\''
                + ", sn='" + sn + '\''
                + ", uid='" + uid + '\''
                + ", givenname='" + givenname + '\''
                + ", memberOfGroup=" + memberOfGroup + '\''
                + ", krbPasswordExpiration='" + krbPasswordExpiration + '\''
                + '}';
    }
}
