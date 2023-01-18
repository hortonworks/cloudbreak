package com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.common.model.annotations.Immutable;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
public class CertificateV4Response {

    @Schema(description = StackModelDescription.CERTIFICATE)
    private String serverCert;

    @Schema(description = StackModelDescription.CLIENT_KEY)
    private String clientKeyPath;

    @Schema(description = StackModelDescription.CLIENT_CERT)
    private String clientCertPath;

    public CertificateV4Response() {
    }

    public CertificateV4Response(String serverCert, String clientKeyPath, String clientCertPath) {
        this.serverCert = serverCert;
        this.clientKeyPath = clientKeyPath;
        this.clientCertPath = clientCertPath;
    }

    public String getServerCert() {
        return serverCert;
    }

    public String getClientKeyPath() {
        return clientKeyPath;
    }

    public String getClientCertPath() {
        return clientCertPath;
    }
}
