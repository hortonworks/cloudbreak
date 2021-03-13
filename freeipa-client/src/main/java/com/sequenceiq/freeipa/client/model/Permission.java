package com.sequenceiq.freeipa.client.model;

import java.util.List;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sequenceiq.freeipa.client.deserializer.ListFlatteningDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Permission {

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String cn;

    private String dn;

    private List<String> ipapermbindruletype;

    private List<String> ipapermissiontype;

    private List<String> ipapermdefaultattr;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String ipapermlocation;

    public String getCn() {
        return cn;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public List<String> getIpapermbindruletype() {
        return ipapermbindruletype;
    }

    public void setIpapermbindruletype(List<String> ipapermbindruletype) {
        this.ipapermbindruletype = ipapermbindruletype;
    }

    public List<String> getIpapermissiontype() {
        return ipapermissiontype;
    }

    public void setIpapermissiontype(List<String> ipapermissiontype) {
        this.ipapermissiontype = ipapermissiontype;
    }

    public List<String> getIpapermdefaultattr() {
        return ipapermdefaultattr;
    }

    public void setIpapermdefaultattr(List<String> ipapermdefaultattr) {
        this.ipapermdefaultattr = ipapermdefaultattr;
    }

    public String getIpapermlocation() {
        return ipapermlocation;
    }

    public void setIpapermlocation(String ipapermlocation) {
        this.ipapermlocation = ipapermlocation;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Permission.class.getSimpleName() + "[", "]")
                .add("cn='" + cn + "'")
                .add("dn='" + dn + "'")
                .add("ipapermbindruletype=" + ipapermbindruletype)
                .add("ipapermissiontype=" + ipapermissiontype)
                .add("ipapermdefaultattr=" + ipapermdefaultattr)
                .add("ipapermlocation='" + ipapermlocation + "'")
                .toString();
    }
}
