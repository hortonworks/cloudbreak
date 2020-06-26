package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.List;

public class Minion {

    private String address;

    /**
     * @deprecated Do not use it, it is deprecated since salt-bootstrap 0.11.0, please use servers
     */
    @Deprecated
    private String server;

    private List<String> servers;

    private String hostGroup;

    private String domain;

    private String hostName;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * @deprecated Do not use it, it is deprecated since salt-bootstrap 0.11.0, please use getServers()
     */
    @Deprecated
    public String getServer() {
        return server;
    }

    /**
     * @deprecated Do not use it, it is deprecated since salt-bootstrap 0.11.0, please use setServers()
     */
    @Deprecated
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

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }
}
