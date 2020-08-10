package com.sequenceiq.freeipa.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sequenceiq.freeipa.client.deserializer.ListFlatteningDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DnsZoneList {

    private String dn;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String idnsname;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getIdnsname() {
        return idnsname;
    }

    public void setIdnsname(String idnsname) {
        this.idnsname = idnsname;
    }

    @Override
    public String toString() {
        return "DnsZoneList{"
                + "dn='" + dn + '\''
                + ", idnsname='" + idnsname + '\''
                + '}';
    }
}
