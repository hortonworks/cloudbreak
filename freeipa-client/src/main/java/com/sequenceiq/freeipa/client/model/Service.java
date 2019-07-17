package com.sequenceiq.freeipa.client.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sequenceiq.freeipa.client.deserializer.ListFlatteningDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Service {

    private String dn;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String krbprincipalname;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String krbcanonicalname;

    @JsonProperty("has_keytab")
    private Boolean hasKeytab = Boolean.FALSE;

    @JsonProperty("memberof_role")
    private List<String> memberOfRole = new ArrayList<>();

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getKrbprincipalname() {
        return krbprincipalname;
    }

    public void setKrbprincipalname(String krbprincipalname) {
        this.krbprincipalname = krbprincipalname;
    }

    public String getKrbcanonicalname() {
        return krbcanonicalname;
    }

    public void setKrbcanonicalname(String krbcanonicalname) {
        this.krbcanonicalname = krbcanonicalname;
    }

    public Boolean getHasKeytab() {
        return hasKeytab;
    }

    public void setHasKeytab(Boolean hasKeytab) {
        this.hasKeytab = hasKeytab;
    }

    public List<String> getMemberOfRole() {
        return memberOfRole;
    }

    public void setMemberOfRole(List<String> memberOfRole) {
        this.memberOfRole = memberOfRole;
    }
}
