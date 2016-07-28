package com.sequenceiq.cloudbreak.api.model;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.AmbariDatabaseDetailsDescription.HOST;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.AmbariDatabaseDetailsDescription.NAME;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.AmbariDatabaseDetailsDescription.PASSWORD;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.AmbariDatabaseDetailsDescription.PORT;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.AmbariDatabaseDetailsDescription.USER_NAME;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.AmbariDatabaseDetailsDescription.VENDOR;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("AmbariDatabaseDetails")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmbariDatabaseDetailsJson {

    @NotNull
    @ApiModelProperty(value = VENDOR, required = true)
    private DatabaseVendor vendor;
    @NotNull
    @Pattern(regexp = "^[^']+$", message = "Invalid character in name: '")
    @ApiModelProperty(value = NAME, required = true)
    private String name;
    @NotNull
    @Pattern(regexp = "^[a-zA-Z0-9]([a-zA-Z0-9-\\.]+)$", message = "The hostname must be valid")
    @ApiModelProperty(value = HOST, required = true)
    private String host;
    @NotNull
    @ApiModelProperty(value = PORT, required = true)
    private Integer port;
    @NotNull
    @Pattern(regexp = "^[^']+$", message = "Invalid character in user name: '")
    @ApiModelProperty(value = USER_NAME, required = true)
    private String userName;
    @NotNull
    @Pattern(regexp = "^[^']+$", message = "Invalid character in password: '")
    @ApiModelProperty(value = PASSWORD, required = true)
    private String password;

    public DatabaseVendor getVendor() {
        return vendor;
    }

    public void setVendor(DatabaseVendor vendor) {
        this.vendor = vendor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
