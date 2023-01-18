package com.sequenceiq.environment.api.v1.credential.model.parameters.azure;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.common.api.credential.AppCertificateStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AzureCertificateResponse")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AzureCredentialCertificateResponse implements Serializable {

    @Schema
    private AppCertificateStatus status;

    @Schema
    private Long expiration;

    @Schema
    private String base64;

    @Schema
    private String sha512;

    public AppCertificateStatus getStatus() {
        return status;
    }

    public void setStatus(AppCertificateStatus status) {
        this.status = status;
    }

    public Long getExpiration() {
        return expiration;
    }

    public void setExpiration(Long expiration) {
        this.expiration = expiration;
    }

    public String getBase64() {
        return base64;
    }

    public void setBase64(String base64) {
        this.base64 = base64;
    }

    public String getSha512() {
        return sha512;
    }

    public void setSha512(String sha512) {
        this.sha512 = sha512;
    }

    @Override
    public String toString() {
        return "AzureCertificateResponse{" +
                ", status='" + status + '\'' +
                ", expiration=" + expiration +
                ", base64='" + base64 + '\'' +
                ", sha512='" + sha512 + '\'' +
                '}';
    }
}
