package com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.requests;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.BlueprintV4Base;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.BlueprintModelDescription;
import com.sequenceiq.cloudbreak.validation.ValidDeprecated;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class BlueprintV4Request extends BlueprintV4Base {

    @ApiModelProperty(value = ModelDescriptions.NAME)
    @Size(max = 100, min = 1, message = "The length of the blueprint's name has to be in range of 1 to 100 and should not contain semicolon "
            + "and percentage character.")
    @Pattern(regexp = "^[^;\\/%]*$")
    private String name;

    @ValidDeprecated(message = "Submitting Cluster Template by URL is not allowed anymore. Please use text based submission.")
    @ApiModelProperty(BlueprintModelDescription.URL)
    private String url;

    private Set<String> services = new HashSet<>();

    private String platform;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Set<String> getServices() {
        return services;
    }

    public void setServices(Set<String> services) {
        this.services = services;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
}
