package com.sequenceiq.cloudbreak.domain;

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
    private String kerberosMasterKey;

    @Type(type = "encrypted_string")
    private String kerberosAdmin;

    @Type(type = "encrypted_string")
    private String kerberosPassword;

    @Type(type = "encrypted_string")
    private String kerberosUrl;

    @Type(type = "encrypted_string")
    private String kerberosRealm;

    private Boolean kerberosTcpAllowed;

    @Type(type = "encrypted_string")
    private String kerberosPrincipal;

    private String kerberosLdapUrl;

    private String kerberosContainerDn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKerberosMasterKey() {
        return kerberosMasterKey;
    }

    public void setKerberosMasterKey(String kerberosMasterKey) {
        this.kerberosMasterKey = kerberosMasterKey;
    }

    public String getKerberosAdmin() {
        return kerberosAdmin;
    }

    public void setKerberosAdmin(String kerberosAdmin) {
        this.kerberosAdmin = kerberosAdmin;
    }

    public String getKerberosPassword() {
        return kerberosPassword;
    }

    public void setKerberosPassword(String kerberosPassword) {
        this.kerberosPassword = kerberosPassword;
    }

    public String getKerberosUrl() {
        return kerberosUrl;
    }

    public void setKerberosUrl(String kerberosUrl) {
        this.kerberosUrl = kerberosUrl;
    }

    public String getKerberosRealm() {
        return kerberosRealm;
    }

    public void setKerberosRealm(String kerberosRealm) {
        this.kerberosRealm = kerberosRealm;
    }

    public Boolean getKerberosTcpAllowed() {
        return kerberosTcpAllowed;
    }

    public void setKerberosTcpAllowed(Boolean kerberosTcpAllowed) {
        this.kerberosTcpAllowed = kerberosTcpAllowed;
    }

    public String getKerberosPrincipal() {
        return kerberosPrincipal;
    }

    public void setKerberosPrincipal(String kerberosPrincipal) {
        this.kerberosPrincipal = kerberosPrincipal;
    }

    public String getKerberosLdapUrl() {
        return kerberosLdapUrl;
    }

    public void setKerberosLdapUrl(String kerberosLdapUrl) {
        this.kerberosLdapUrl = kerberosLdapUrl;
    }

    public String getKerberosContainerDn() {
        return kerberosContainerDn;
    }

    public void setKerberosContainerDn(String kerberosContainerDn) {
        this.kerberosContainerDn = kerberosContainerDn;
    }
}
