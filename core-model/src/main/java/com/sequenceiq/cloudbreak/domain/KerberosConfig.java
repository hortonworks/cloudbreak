package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

@Entity
@Table(name = "KerberosConfig")
public class KerberosConfig implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "kerberosconfig_generator")
    @SequenceGenerator(name = "kerberosconfig_generator", sequenceName = "kerberosconfig_id_seq", allocationSize = 1)
    private Long id;

    @Type(type = "encrypted_string")
    @Column(name = "kerberosmasterkey")
    private String masterKey;

    @Type(type = "encrypted_string")
    @Column(name = "kerberosadmin")
    private String admin;

    @Type(type = "encrypted_string")
    @Column(name = "kerberospassword")
    private String password;

    @Type(type = "encrypted_string")
    @Column(name = "kerberosurl")
    private String url;

    @Type(type = "encrypted_string")
    @Column(name = "kdcadminurl")
    private String adminUrl;

    @Type(type = "encrypted_string")
    @Column(name = "kerberosrealm")
    private String realm;

    @Column(name = "kerberostcpallowed")
    private Boolean tcpAllowed;

    @Type(type = "encrypted_string")
    @Column(name = "kerberosprincipal")
    private String principal;

    @Column(name = "kerberosldapurl")
    private String ldapUrl;

    @Column(name = "kerberoscontainerdn")
    private String containerDn;

    @Type(type = "encrypted_string")
    @Column(name = "kerberosdescriptor", columnDefinition = "TEXT")
    private String descriptor;

    @Type(type = "encrypted_string")
    @Column(name = "krb5conf", columnDefinition = "TEXT")
    private String krb5Conf;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMasterKey() {
        return masterKey;
    }

    public void setMasterKey(String masterKey) {
        this.masterKey = masterKey;
    }

    public String getAdmin() {
        return admin;
    }

    public void setAdmin(String admin) {
        this.admin = admin;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
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

    public Boolean getTcpAllowed() {
        return tcpAllowed;
    }

    public void setTcpAllowed(Boolean tcpAllowed) {
        this.tcpAllowed = tcpAllowed;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
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

    public String getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }

    public String getKrb5Conf() {
        return krb5Conf;
    }

    public void setKrb5Conf(String krb5Conf) {
        this.krb5Conf = krb5Conf;
    }
}
