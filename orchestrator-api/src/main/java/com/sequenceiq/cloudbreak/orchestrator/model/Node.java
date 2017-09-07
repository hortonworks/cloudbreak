package com.sequenceiq.cloudbreak.orchestrator.model;

import java.util.Set;

public class Node {
    private final String privateIp;

    private final String publicIp;

    private String hostname;

    private String domain;

    private String hostGroup;

    private Set<String> dataVolumes;

    public Node(String privateIp, String publicIp, String fqdn, String hostGroup) {
        this(privateIp, publicIp, fqdn, null, hostGroup);
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

    public Node(String privateIp, String publicIp, String hostname, Set<String> dataVolumes) {
        this.privateIp = privateIp;
        this.publicIp = publicIp;
        this.hostname = hostname;
        this.dataVolumes = dataVolumes;
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

    public Set<String> getDataVolumes() {
        return dataVolumes;
    }

    public String getHostGroup() {
        return hostGroup;
    }

    public String getDomain() {
        return domain;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Node{");
        sb.append("privateIp='").append(privateIp).append('\'');
        sb.append(", publicIp='").append(publicIp).append('\'');
        sb.append(", hostname='").append(hostname).append('\'');
        sb.append(", domain='").append(domain).append('\'');
        sb.append(", hostGroup='").append(hostGroup).append('\'');
        sb.append(", dataVolumes=").append(dataVolumes);
        sb.append('}');
        return sb.toString();
    }
}
