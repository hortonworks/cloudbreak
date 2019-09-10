package com.sequenceiq.cloudbreak.domain;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;

@Entity
public class SecurityConfig implements ProvisionEntity, WorkspaceAwareResource {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "securityconfig_generator")
    @SequenceGenerator(name = "securityconfig_generator", sequenceName = "securityconfig_id_seq", allocationSize = 1)
    private Long id;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret clientKey = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret clientCert = Secret.EMPTY;

    @OneToOne(fetch = FetchType.LAZY)
    private Stack stack;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private SaltSecurityConfig saltSecurityConfig;

    @Column(nullable = false)
    private boolean usePrivateIpToTls;

    @ManyToOne
    private Workspace workspace;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret userFacingCert = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret userFacingKey = Secret.EMPTY;

    public Long getId() {
        return id;
    }

    @Override
    public Workspace getWorkspace() {
        return workspace;
    }

    @Override
    public String getName() {
        return "securityconfig-" + id;
    }

    @Override
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setClientKey(String clientKey) {
        this.clientKey = new Secret(clientKey);
    }

    public void setClientKey(Secret clientKey) {
        this.clientKey = clientKey;
    }

    public String getClientKey() {
        return clientKey.getRaw();
    }

    public String getClientKeySecret() {
        return clientKey.getSecret();
    }

    public String getClientCert() {
        return clientCert.getRaw();
    }

    public String getClientCertSecret() {
        return clientCert.getSecret();
    }

    public void setClientCert(String clientCert) {
        this.clientCert = new Secret(clientCert);
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    public SaltSecurityConfig getSaltSecurityConfig() {
        return saltSecurityConfig;
    }

    public void setSaltSecurityConfig(SaltSecurityConfig saltSecurityConfig) {
        this.saltSecurityConfig = saltSecurityConfig;
    }

    public boolean isUsePrivateIpToTls() {
        return usePrivateIpToTls;
    }

    public void setUsePrivateIpToTls(boolean usePrivateIpToTls) {
        this.usePrivateIpToTls = usePrivateIpToTls;
    }

    public String getUserFacingCert() {
        return userFacingCert.getRaw();
    }

    public void setUserFacingCert(String userFacingCert) {
        this.userFacingCert = new Secret(userFacingCert);
    }

    public String getUserFacingKey() {
        return userFacingKey.getRaw();
    }

    public void setUserFacingKey(String userFacingKey) {
        this.userFacingKey = new Secret(userFacingKey);
    }
}
