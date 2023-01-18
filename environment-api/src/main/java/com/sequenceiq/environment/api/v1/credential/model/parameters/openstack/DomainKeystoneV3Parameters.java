package com.sequenceiq.environment.api.v1.credential.model.parameters.openstack;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(allOf = KeystoneV3Base.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Deprecated
public class DomainKeystoneV3Parameters extends KeystoneV3Base {

    @NotNull
    @Schema(required = true)
    private String domainName;

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    @Override
    public String toString() {
        return super.toString() + ", " + "DomainKeystoneV3Parameters{" +
                "domainName='" + domainName + '\'' +
                '}';
    }
}
