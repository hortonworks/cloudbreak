package com.sequenceiq.cloudbreak.common.orchestration;

import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;

public class Node {
    private final String privateIp;

    private final String publicIp;

    private final String instanceId;

    private final String instanceType;

    private String hostname;

    private String domain;

    private String hostGroup;

    private NodeVolumes nodeVolumes;

    private TemporaryStorage temporaryStorage;

    public Node(String privateIp, String publicIp, String instanceId, String instanceType, String fqdn, String hostGroup) {
        this(privateIp, publicIp, instanceId, instanceType, fqdn, null, hostGroup);
    }

    public Node(String privateIp, String publicIp, String instanceId, String instanceType, String fqdn, String hostGroup, NodeVolumes nodeVolumes,
            TemporaryStorage temporaryStorage) {
        this(privateIp, publicIp, instanceId, instanceType, fqdn, null, hostGroup);
        this.nodeVolumes = nodeVolumes;
        this.temporaryStorage = temporaryStorage;
    }

    public Node(String privateIp, String publicIp, String instanceId, String instanceType, String fqdn, String domain, String hostGroup) {
        this(privateIp, publicIp, instanceId, instanceType);
        hostname = fqdn;
        this.hostGroup = hostGroup;
        this.domain = domain;
    }

    public Node(String privateIp, String publicIp, String instanceId, String instanceType) {
        this.privateIp = privateIp;
        this.publicIp = publicIp;
        this.instanceId = instanceId;
        this.instanceType = instanceType;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getHostGroup() {
        return hostGroup;
    }

    public String getDomain() {
        return domain;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public NodeVolumes getNodeVolumes() {
        return nodeVolumes;
    }

    public TemporaryStorage getTemporaryStorage() {
        return temporaryStorage;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Node{");
        sb.append("privateIp='").append(privateIp).append('\'');
        sb.append(", publicIp='").append(publicIp).append('\'');
        sb.append(", instanceId='").append(instanceId).append('\'');
        sb.append(", instanceType='").append(instanceType).append('\'');
        sb.append(", hostname='").append(hostname).append('\'');
        sb.append(", domain='").append(domain).append('\'');
        sb.append(", hostGroup='").append(hostGroup).append('\'');
        sb.append(", nodeVolumes=").append(nodeVolumes).append('\'');
        sb.append(", temporaryStorage=").append(temporaryStorage);
        sb.append('}');
        return sb.toString();
    }
}
