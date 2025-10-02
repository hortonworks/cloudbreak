package com.sequenceiq.cloudbreak.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.converter.SeLinuxConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;
import com.sequenceiq.common.model.SeLinux;

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

    @SecretValue
    @Convert(converter = SecretToString.class)
    private Secret alternativeUserFacingCert = Secret.EMPTY;

    @SecretValue
    @Convert(converter = SecretToString.class)
    private Secret alternativeUserFacingKey = Secret.EMPTY;

    @Convert(converter = SeLinuxConverter.class)
    @Column(name = "selinux")
    private SeLinux seLinux = SeLinux.PERMISSIVE;

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

    public String getAlternativeUserFacingCert() {
        return alternativeUserFacingCert.getRaw();
    }

    public void setAlternativeUserFacingCert(String alternativeUserFacingCert) {
        this.alternativeUserFacingCert = new Secret(alternativeUserFacingCert);
    }

    public String getAlternativeUserFacingKey() {
        return alternativeUserFacingKey.getRaw();
    }

    public void setAlternativeUserFacingKey(String alternativeUserFacingKey) {
        this.alternativeUserFacingKey = new Secret(alternativeUserFacingKey);
    }

    public SeLinux getSeLinux() {
        return seLinux;
    }

    public void setSeLinux(SeLinux seLinux) {
        this.seLinux = seLinux;
    }
}
