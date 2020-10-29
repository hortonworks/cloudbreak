package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServer;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = ModelDescriptions.SSL_CONFIG_REQUEST)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SslConfigV4Request {

    @ApiModelProperty(DatabaseServer.SSL_MODE)
    private SslMode sslMode;

    public SslMode getSslMode() {
        return sslMode;
    }

    public void setSslMode(SslMode sslMode) {
        this.sslMode = sslMode;
    }

}
