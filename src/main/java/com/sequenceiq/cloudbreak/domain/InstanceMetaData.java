package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@NamedQueries({
        @NamedQuery(
                name = "InstanceMetaData.findHostInStack",
                query = "SELECT i FROM InstanceMetaData i "
                        + "WHERE i.stack.id= :stackId "
                        + "AND i.longName= :hostName")

})
public class InstanceMetaData implements ProvisionEntity {

    @Id
    @GeneratedValue
    private Long id;
    private String privateIp;
    private String publicIp;
    private Integer volumeCount;
    private String instanceId;
    private int instanceIndex;
    private Boolean ambariServer;
    private String dockerSubnet;
    private String longName;
    private Boolean removable;
    @ManyToOne
    private Stack stack;

    public InstanceMetaData() {

    }

    public String getPrivateIp() {
        return privateIp;
    }

    public void setPrivateIp(String privateIp) {
        this.privateIp = privateIp;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public Integer getVolumeCount() {
        return volumeCount;
    }

    public void setVolumeCount(Integer volumeCount) {
        this.volumeCount = volumeCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public int getInstanceIndex() {
        return instanceIndex;
    }

    public void setInstanceIndex(int instanceIndex) {
        this.instanceIndex = instanceIndex;
    }

    public Boolean getAmbariServer() {
        return ambariServer;
    }

    public void setAmbariServer(Boolean ambariServer) {
        this.ambariServer = ambariServer;
    }

    public String getDockerSubnet() {
        return dockerSubnet;
    }

    public void setDockerSubnet(String dockerSubnet) {
        this.dockerSubnet = dockerSubnet;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    public String getLongName() {
        return longName;
    }

    public void setLongName(String longName) {
        this.longName = longName;
    }

    public Boolean isRemovable() {
        return removable;
    }

    public void setRemovable(Boolean removable) {
        this.removable = removable;
    }
}
