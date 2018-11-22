package com.sequenceiq.cloudbreak.orchestrator.model;

public class Node {
    private final String privateIp;

    private final String publicIp;

    private String hostname;

    private String domain;

    private String hostGroup;

    private String dataVolumes;

    private String serialIds;

    private String fstab;

    private String uuids;

    public Node(String privateIp, String publicIp, String fqdn, String hostGroup) {
        this(privateIp, publicIp, fqdn, null, hostGroup);
    }

    public Node(String privateIp, String publicIp, String fqdn, String hostGroup, String dataVolumes, String serialIds, String fstab, String uuids) {
        this(privateIp, publicIp, fqdn, null, hostGroup);
        this.dataVolumes = dataVolumes;
        this.serialIds = serialIds;
        this.fstab = fstab;
        this.uuids = uuids;
    }

    public Node(String privateIp, String publicIp, String fqdn, String domain, String hostGroup) {
        this(privateIp, publicIp);
        hostname = fqdn;
        this.hostGroup = hostGroup;
        this.domain = domain;
    }

    public Node(String privateIp, String publicIp) {
        this.privateIp = privateIp;
        this.publicIp = publicIp;
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

    public String getDataVolumes() {
        return dataVolumes;
    }

    public String getSerialIds() {
        return serialIds;
    }

    public String getHostGroup() {
        return hostGroup;
    }

    public String getDomain() {
        return domain;
    }

    public String getFstab() {
        return fstab;
    }

    public String getUuids() {
        return uuids;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Node{");
        sb.append("privateIp='").append(privateIp).append('\'');
        sb.append(", publicIp='").append(publicIp).append('\'');
        sb.append(", hostname='").append(hostname).append('\'');
        sb.append(", domain='").append(domain).append('\'');
        sb.append(", hostGroup='").append(hostGroup).append('\'');
        sb.append(", dataVolumes='").append(dataVolumes).append('\'');
        sb.append(", serialIds='").append(serialIds).append('\'');
        sb.append(", fstab='").append(fstab).append('\'');
        sb.append(", uuids='").append(uuids).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
