package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

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

    private boolean restartNeeded;

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

    /**
     * FreeIPA requires hostname to be the same as FQDN. Minion ID is the FQDN of the host.
     * To handle this scenario, we only append domain to hostname if the hostname is not the FQDN already.
     * @return FQDN of the host
     */
    public String getId() {
        return StringUtils.endsWith(hostName, domain) ? hostName : hostName + '.' + domain;
    }

    public boolean isRestartNeeded() {
        return restartNeeded;
    }

    public void setRestartNeeded(boolean restartNeeded) {
        this.restartNeeded = restartNeeded;
    }

    @Override
    public String toString() {
        return "Minion{" +
                "address='" + address + '\'' +
                ", server='" + server + '\'' +
                ", servers=" + servers +
                ", hostGroup='" + hostGroup + '\'' +
                ", domain='" + domain + '\'' +
                ", hostName='" + hostName + '\'' +
                ", id='" + getId() + '\'' +
                '}';
    }
}
