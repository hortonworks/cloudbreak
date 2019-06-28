package com.sequenceiq.freeipa.client.model;

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
    private Boolean hasKeytab;

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
}
