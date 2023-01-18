package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServer;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.SSL_CONFIG_REQUEST)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SslConfigV4Request {

    @Schema(description = DatabaseServer.SSL_MODE)
    private SslMode sslMode;

    public SslMode getSslMode() {
        return sslMode;
    }

    public void setSslMode(SslMode sslMode) {
        this.sslMode = sslMode;
    }

    @Override
    public String toString() {
        return "SslConfigV4Request{" +
                "sslMode=" + sslMode +
                '}';
    }
}
