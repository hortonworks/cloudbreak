package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.redbeams.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;

@ApiModel(description = ModelDescriptions.SSL_CONFIGURATION_REQUEST)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SslConfigurationV4Request {

    private SslMode sslMode;

    public SslMode getSslMode() {
        return sslMode;
    }

    public void setSslMode(SslMode sslMode) {
        this.sslMode = sslMode;
    }
}
