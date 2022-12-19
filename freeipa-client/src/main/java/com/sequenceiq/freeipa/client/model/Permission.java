package com.sequenceiq.freeipa.client.model;

import java.util.List;

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

    private List<String> ipapermincludedattr;

    private List<String> ipapermright;

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

    public List<String> getIpapermincludedattr() {
        return ipapermincludedattr;
    }

    public void setIpapermincludedattr(List<String> ipapermincludedattr) {
        this.ipapermincludedattr = ipapermincludedattr;
    }

    public List<String> getIpapermright() {
        return ipapermright;
    }

    public void setIpapermright(List<String> ipapermright) {
        this.ipapermright = ipapermright;
    }

    @Override
    public String toString() {
        return "Permission{" +
                "cn='" + cn + '\'' +
                ", dn='" + dn + '\'' +
                ", ipapermbindruletype=" + ipapermbindruletype +
                ", ipapermissiontype=" + ipapermissiontype +
                ", ipapermdefaultattr=" + ipapermdefaultattr +
                ", ipapermlocation='" + ipapermlocation + '\'' +
                ", ipapermincludedattr=" + ipapermincludedattr +
                ", ipapermright=" + ipapermright +
                '}';
    }
}
