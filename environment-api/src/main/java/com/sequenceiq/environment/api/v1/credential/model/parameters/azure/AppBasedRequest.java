package com.sequenceiq.environment.api.v1.credential.model.parameters.azure;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.common.api.credential.AppAuthenticationType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AppBasedV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AppBasedRequest implements Serializable {

    @Schema
    private String accessKey;

    // We should default to SECRET, since in the past we supported only SECRET based app credentials
    @Schema
    private AppAuthenticationType authenticationType = AppAuthenticationType.SECRET;

    @Schema
    private Boolean generateCertificate;

    @Schema
    private String secretKey;

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public AppAuthenticationType getAuthenticationType() {
        return authenticationType;
    }

    public void setAuthenticationType(AppAuthenticationType authenticationType) {
        this.authenticationType = authenticationType;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public Boolean getGenerateCertificate() {
        return generateCertificate;
    }

    public void setGenerateCertificate(Boolean generateCertificate) {
        this.generateCertificate = generateCertificate;
    }

    @Override
    public String toString() {
        return "AppBasedRequest{" +
                "accessKey='" + accessKey + '\'' +
                ", authenticationType=" + authenticationType +
                ", generateCertificate=" + generateCertificate +
                '}';
    }
}
