package com.sequenceiq.cloudbreak.api.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
abstract class SssdConfigBase implements JsonEntity {

    @Size(max = 100, min = 1, message = "The length of the config's name has to be in range of 1 to 100")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])",
            message = "The config's name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;
    @Size(max = 1000, message = "The length of the config's description has to be less than 1000")
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.SssdConfigModelDescription.PROVIDER_TYPE, required = true)
    private SssdProviderType providerType;
    @NotNull
    @Size(min = 10, max = 255, message = "The length of the config's url has to be in range of 10 to 255")
    @ApiModelProperty(value = ModelDescriptions.SssdConfigModelDescription.URL, required = true)
    private String url;
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.SssdConfigModelDescription.SCHEMA, required = true)
    private SssdSchemaType schema;
    @NotNull
    @Size(min = 10, max = 255, message = "The length of the config's search base has to be in range of 10 to 255")
    @ApiModelProperty(value = ModelDescriptions.SssdConfigModelDescription.BASE_SEARCH, required = true)
    private String baseSearch;
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.SssdConfigModelDescription.TLS_REQUCERT, required = true)
    private SssdTlsReqcert tlsReqcert;
    @ApiModelProperty(value = ModelDescriptions.SssdConfigModelDescription.AD_SERVER)
    private String adServer;
    @ApiModelProperty(value = ModelDescriptions.SssdConfigModelDescription.KERBEROS_SERVER)
    private String kerberosServer;
    @ApiModelProperty(value = ModelDescriptions.SssdConfigModelDescription.KERBEROS_REALM)
    private String kerberosRealm;
    @ApiModelProperty(value = ModelDescriptions.SssdConfigModelDescription.CONFIGURATION)
    private String configuration;

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

    public SssdTlsReqcert getTlsReqcert() {
        return tlsReqcert;
    }

    public void setTlsReqcert(SssdTlsReqcert tlsReqcert) {
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
