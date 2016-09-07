package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.api.model.SssdProviderType;
import com.sequenceiq.cloudbreak.api.model.SssdSchemaType;
import com.sequenceiq.cloudbreak.api.model.SssdTlsReqcertType;

@Entity
@Table(name = "sssdconfig", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "account", "name" })
})
@NamedQueries({
        @NamedQuery(
                name = "SssdConfig.findForUser",
                query = "SELECT c FROM SssdConfig c "
                        + "WHERE c.owner= :owner"),
        @NamedQuery(
                name = "SssdConfig.findPublicInAccountForUser",
                query = "SELECT c FROM SssdConfig c "
                        + "WHERE (c.account= :account AND c.publicInAccount= true) "
                        + "OR c.owner= :owner"),
        @NamedQuery(
                name = "SssdConfig.findAllInAccount",
                query = "SELECT c FROM SssdConfig c "
                        + "WHERE c.account= :account "),
        @NamedQuery(
                name = "SssdConfig.findByNameForUser",
                query = "SELECT c FROM SssdConfig c "
                        + "WHERE c.name= :name and c.owner= :owner "),
        @NamedQuery(
                name = "SssdConfig.findByNameInAccount",
                query = "SELECT c FROM SssdConfig c WHERE c.name= :name and c.account= :account")
})
public class SssdConfig implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sssdconfig_generator")
    @SequenceGenerator(name = "sssdconfig_generator", sequenceName = "sssdconfig_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String account;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private boolean publicInAccount;

    @Enumerated(EnumType.STRING)
    private SssdProviderType providerType;

    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "ldapschema")
    private SssdSchemaType schema;

    @Column(length = 500, columnDefinition = "TEXT")
    private String baseSearch;

    @Enumerated(EnumType.STRING)
    private SssdTlsReqcertType tlsReqcert;

    private String adServer;

    private String kerberosServer;

    private String kerberosRealm;

    @Column(length = 1000, columnDefinition = "TEXT")
    private String configuration;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
    }

    public SssdProviderType getProviderType() {
        return providerType;
    }

    public void setProviderType(SssdProviderType providerType) {
        this.providerType = providerType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public SssdSchemaType getSchema() {
        return schema;
    }

    public void setSchema(SssdSchemaType schema) {
        this.schema = schema;
    }

    public String getBaseSearch() {
        return baseSearch;
    }

    public void setBaseSearch(String baseSearch) {
        this.baseSearch = baseSearch;
    }

    public SssdTlsReqcertType getTlsReqcert() {
        return tlsReqcert;
    }

    public void setTlsReqcert(SssdTlsReqcertType tlsReqcert) {
        this.tlsReqcert = tlsReqcert;
    }

    public String getAdServer() {
        return adServer;
    }

    public void setAdServer(String adServer) {
        this.adServer = adServer;
    }

    public String getKerberosServer() {
        return kerberosServer;
    }

    public void setKerberosServer(String kerberosServer) {
        this.kerberosServer = kerberosServer;
    }

    public String getKerberosRealm() {
        return kerberosRealm;
    }

    public void setKerberosRealm(String kerberosRealm) {
        this.kerberosRealm = kerberosRealm;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }
}
