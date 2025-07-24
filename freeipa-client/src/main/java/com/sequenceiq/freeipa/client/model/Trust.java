package com.sequenceiq.freeipa.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sequenceiq.freeipa.client.deserializer.ListFlatteningDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Trust {
    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String cn;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String ipantflatname;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String trustdirection;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String trusttype;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String truststatus;

    public String getCn() {
        return cn;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }

    public String getIpantflatname() {
        return ipantflatname;
    }

    public void setIpantflatname(String ipantflatname) {
        this.ipantflatname = ipantflatname;
    }

    public String getTrustdirection() {
        return trustdirection;
    }

    public void setTrustdirection(String trustdirection) {
        this.trustdirection = trustdirection;
    }

    public String getTrusttype() {
        return trusttype;
    }

    public void setTrusttype(String trusttype) {
        this.trusttype = trusttype;
    }

    public String getTruststatus() {
        return truststatus;
    }

    public void setTruststatus(String truststatus) {
        this.truststatus = truststatus;
    }

    @Override
    public String toString() {
        return "Trust{" +
                "cn='" + cn + '\'' +
                ", ipantflatname='" + ipantflatname + '\'' +
                ", trustdirection='" + trustdirection + '\'' +
                ", trusttype='" + trusttype + '\'' +
                ", truststatus='" + truststatus + '\'' +
                '}';
    }
}
