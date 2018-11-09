package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.aspect.secret.SecretValue;
import com.sequenceiq.cloudbreak.type.KerberosType;

@Entity
public class KerberosConfig implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "kerberosconfig_generator")
    @SequenceGenerator(name = "kerberosconfig_generator", sequenceName = "kerberosconfig_id_seq", allocationSize = 1)
    private Long id;

    @Enumerated(EnumType.STRING)
    private KerberosType type;

    @Column(name = "kerberosmasterkey")
    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret masterKey = Secret.EMPTY;

    @Column(name = "kerberosadmin")
    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret admin = Secret.EMPTY;

    @Column(name = "kerberospassword")
    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret password = Secret.EMPTY;

    @Column(name = "kerberosurl")
    private String url;

    @Column(name = "kdcadminurl")
    private String adminUrl;

    @Column(name = "kerberosrealm")
    private String realm;

    @Column(name = "kerberostcpallowed")
    private Boolean tcpAllowed;

    @Column(name = "kerberosprincipal")
    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret principal = Secret.EMPTY;

    @Column(name = "kerberosldapurl")
    private String ldapUrl;

    @Column(name = "kerberoscontainerdn")
    private String containerDn;

    @Column(name = "kerberosdescriptor")
    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret descriptor = Secret.EMPTY;

    @Column(name = "krb5conf")
    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret krb5Conf = Secret.EMPTY;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public KerberosType getType() {
        return type;
    }

    public void setType(KerberosType type) {
        this.type = type;
    }

    public Secret getMasterKey() {
        return masterKey;
    }

    public void setMasterKey(String masterKey) {
        this.masterKey = new Secret(masterKey);
    }

    public void setMasterKey(Secret masterKey) {
        this.masterKey = masterKey;
    }

    public Secret getAdmin() {
        return admin;
    }

    public void setAdmin(String admin) {
        this.admin = new Secret(admin);
    }

    public void setAdmin(Secret admin) {
        this.admin = admin;
    }

    public Secret getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = new Secret(password);
    }

    public void setPassword(Secret password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAdminUrl() {
        return adminUrl;
    }

    public void setAdminUrl(String adminUrl) {
        this.adminUrl = adminUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public Boolean isTcpAllowed() {
        return tcpAllowed;
    }

    public void setTcpAllowed(Boolean tcpAllowed) {
        this.tcpAllowed = tcpAllowed;
    }

    public Secret getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = new Secret(principal);
    }

    public void setPrincipal(Secret principal) {
        this.principal = principal;
    }

    public String getLdapUrl() {
        return ldapUrl;
    }

    public void setLdapUrl(String ldapUrl) {
        this.ldapUrl = ldapUrl;
    }

    public String getContainerDn() {
        return containerDn;
    }

    public void setContainerDn(String containerDn) {
        this.containerDn = containerDn;
    }

    public Secret getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = new Secret(descriptor);
    }

    public void setDescriptor(Secret descriptor) {
        this.descriptor = descriptor;
    }

    public Secret getKrb5Conf() {
        return krb5Conf;
    }

    public void setKrb5Conf(String krb5Conf) {
        this.krb5Conf = new Secret(krb5Conf);
    }

    public void setKrb5Conf(Secret krb5Conf) {
        this.krb5Conf = krb5Conf;
    }
}
