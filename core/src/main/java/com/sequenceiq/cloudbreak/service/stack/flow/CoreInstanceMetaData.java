package com.sequenceiq.cloudbreak.service.stack.flow;

public class CoreInstanceMetaData {

    private String instanceId;
    private String privateIp;
    private String publicIp;
    private Long privateId;
    private String instanceGroupName;
    private String hypervisor;

    public CoreInstanceMetaData(String instanceId, Long privateId, String privateIp, String publicIp, String instanceGroupName, String hypervisor) {
        this.instanceId = instanceId;
        this.privateId = privateId;
        this.privateIp = privateIp;
        this.publicIp = publicIp;
        this.instanceGroupName = instanceGroupName;
        this.hypervisor = hypervisor;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public String getInstanceGroupName() {
        return instanceGroupName;
    }

    public Long getPrivateId() {
        return privateId;
    }

    public String getHypervisor() {
        return hypervisor;
    }
}
