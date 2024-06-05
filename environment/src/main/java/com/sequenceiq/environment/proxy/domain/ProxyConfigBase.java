package com.sequenceiq.environment.proxy.domain;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.SequenceGenerator;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.auth.security.AuthResource;
import com.sequenceiq.cloudbreak.common.dal.model.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.util.CidrUtil;

@MappedSuperclass
public class ProxyConfigBase implements Serializable, AuthResource, AccountIdAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "proxyconfig_generator")
    @SequenceGenerator(name = "proxyconfig_generator", sequenceName = "proxyconfig_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String serverHost;

    @Column(nullable = false)
    private Integer serverPort;

    @Column(nullable = false)
    private String protocol;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String resourceCrn;

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
    @Column(nullable = false)
    private String creator;

    private boolean archived;

    private Long deletionTimestamp = -1L;

    private String noProxyHosts;

    private String inboundProxyCidr;

    @Override
    public String getAccountId() {
        return accountId;
    }

    @Override
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isArchived() {
        return archived;
    }

    public Long getDeletionTimestamp() {
        return deletionTimestamp;
    }

    @Override
    public String getResourceCrn() {
        return resourceCrn;
    }

    @Override
    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
    public String getCreator() {
        return creator;
    }

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getNoProxyHosts() {
        return noProxyHosts;
    }

    public void setNoProxyHosts(String noProxyHosts) {
        this.noProxyHosts = noProxyHosts;
    }

    public List<String> getInboundProxyCidr() {
        if (StringUtils.isEmpty(inboundProxyCidr)) {
            return List.of();
        } else {
            return CidrUtil.cidrList(inboundProxyCidr);
        }
    }

    public void setInboundProxyCidr(List<String> inboundProxyCidr) {
        if (CollectionUtils.isEmpty(inboundProxyCidr)) {
            this.inboundProxyCidr = null;
        } else {
            this.inboundProxyCidr = StringUtils.join(inboundProxyCidr, ",");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProxyConfigBase that = (ProxyConfigBase) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ProxyConfigBase{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", serverHost='" + serverHost + '\'' +
                ", serverPort=" + serverPort +
                ", protocol='" + protocol + '\'' +
                ", accountId='" + accountId + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", creator='" + creator + '\'' +
                ", archived=" + archived +
                ", deletionTimestamp=" + deletionTimestamp +
                ", noProxyHosts='" + noProxyHosts + '\'' +
                ", inboundProxyCidr='" + inboundProxyCidr + '\'' +
                '}';
    }
}
