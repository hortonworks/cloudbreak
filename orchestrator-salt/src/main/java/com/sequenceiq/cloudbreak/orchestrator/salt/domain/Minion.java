package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.List;

public class Minion {

    private String address;

    private List<String> roles;

    /**
     * @deprecated Do not use it, it is deprecated since salt-bootstrap 0.11.0, please use servers
     */
    private String server;

    private List<String> servers;

    private String hostGroup;

    private String domain;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    /**
     * @deprecated Do not use it, it is deprecated since salt-bootstrap 0.11.0, please use getServers()
     */
    public String getServer() {
        return server;
    }

    /**
     * @deprecated Do not use it, it is deprecated since salt-bootstrap 0.11.0, please use setServers()
     */
    public void setServer(String server) {
        this.server = server;
    }

    public String getHostGroup() {
        return hostGroup;
    }

    public void setHostGroup(String hostGroup) {
        this.hostGroup = hostGroup;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public List<String> getServers() {
        return servers;
    }

    public void setServers(List<String> servers) {
        this.servers = servers;
    }
}
