package com.sequenceiq.freeipa.client.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sequenceiq.freeipa.client.deserializer.ListFlatteningDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Host {

    private String dn;

    private List<String> sshpubkeyfp;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String krbprincipalname;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String fqdn;

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

    public List<String> getSshpubkeyfp() {
        return sshpubkeyfp;
    }

    public void setSshpubkeyfp(List<String> sshpubkeyfp) {
        this.sshpubkeyfp = sshpubkeyfp;
    }

    public String getKrbprincipalname() {
        return krbprincipalname;
    }

    public void setKrbprincipalname(String krbprincipalname) {
        this.krbprincipalname = krbprincipalname;
    }

    public String getFqdn() {
        return fqdn;
    }

    public void setFqdn(String fqdn) {
        this.fqdn = fqdn;
    }

    public String getKrbcanonicalname() {
        return krbcanonicalname;
    }

    public void setHasKeytab(Boolean hasKeytab) {
        this.hasKeytab = hasKeytab;
    }

    public Boolean getHasKeytab() {
        return hasKeytab;
    }

    public List<String> getMemberOfRole() {
        return memberOfRole;
    }

    public void setMemberOfRole(List<String> memberOfRole) {
        this.memberOfRole = memberOfRole;
    }
}
