package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

@Entity
@Table(name = "SecurityConfig")
@NamedQuery(
        name = "SecurityConfig.getServerCertByStackId",
        query = "SELECT s.serverCert FROM SecurityConfig s "
                + "WHERE s.stack.id= :stackId")
public class SecurityConfig implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "securityconfig_generator")
    @SequenceGenerator(name = "securityconfig_generator", sequenceName = "securityconfig_id_seq", allocationSize = 1)
    private Long id;

    @Type(type = "encrypted_string")
    @Column(columnDefinition = "TEXT")
    private String clientKey;

    @Type(type = "encrypted_string")
    @Column(columnDefinition = "TEXT")
    private String clientCert;

    @Type(type = "encrypted_string")
    @Column(columnDefinition = "TEXT")
    private String serverCert;

    @Column(columnDefinition = "TEXT")
    private String cloudbreakSshPublicKey;

    @Type(type = "encrypted_string")
    @Column(columnDefinition = "TEXT")
    private String cloudbreakSshPrivateKey;

    @Type(type = "encrypted_string")
    private String saltPassword;

    @Type(type = "encrypted_string")
    private String saltBootPassword;

    @OneToOne(fetch = FetchType.LAZY)
    private Stack stack;

    @Column(nullable = false)
    private boolean usePrivateIpToTls;

    public SecurityConfig() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClientKey() {
        return clientKey;
    }

    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }

    public String getClientCert() {
        return clientCert;
    }

    public void setClientCert(String clientCert) {
        this.clientCert = clientCert;
    }

    public String getServerCert() {
        return serverCert;
    }

    public void setServerCert(String serverCert) {
        this.serverCert = serverCert;
    }

    public String getCloudbreakSshPublicKey() {
        return cloudbreakSshPublicKey;
    }

    public void setCloudbreakSshPublicKey(String cloudbreakSshPublicKey) {
        this.cloudbreakSshPublicKey = cloudbreakSshPublicKey;
    }

    public String getCloudbreakSshPrivateKey() {
        return cloudbreakSshPrivateKey;
    }

    public void setCloudbreakSshPrivateKey(String cloudbreakSshPrivateKey) {
        this.cloudbreakSshPrivateKey = cloudbreakSshPrivateKey;
    }

    public String getSaltPassword() {
        return saltPassword;
    }

    public void setSaltPassword(String saltPassword) {
        this.saltPassword = saltPassword;
    }

    public String getSaltBootPassword() {
        return saltBootPassword;
    }

    public void setSaltBootPassword(String saltBootPassword) {
        this.saltBootPassword = saltBootPassword;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    public boolean usePrivateIpToTls() {
        return usePrivateIpToTls;
    }

    public void setUsePrivateIpToTls(boolean usePrivateIpToTls) {
        this.usePrivateIpToTls = usePrivateIpToTls;
    }
}
