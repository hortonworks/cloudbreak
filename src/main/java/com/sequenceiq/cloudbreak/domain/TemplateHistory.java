package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "templatehistory")
public class TemplateHistory extends AbstractHistory {

    private String dType;
    private String amiid;
    private String instancetype;
    private String region;
    private String sshLocation;
    private String imageName;
    private String location;
    private String vmType;

    public String getdType() {
        return dType;
    }

    public void setdType(String dType) {
        this.dType = dType;
    }

    public String getAmiid() {
        return amiid;
    }

    public void setAmiid(String amiid) {
        this.amiid = amiid;
    }

    public String getInstancetype() {
        return instancetype;
    }

    public void setInstancetype(String instancetype) {
        this.instancetype = instancetype;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getSshLocation() {
        return sshLocation;
    }

    public void setSshLocation(String sshLocation) {
        this.sshLocation = sshLocation;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getVmType() {
        return vmType;
    }

    public void setVmType(String vmType) {
        this.vmType = vmType;
    }
}
