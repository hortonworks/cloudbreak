package com.sequenceiq.cloudbreak.api.model


import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription
import io.swagger.annotations.ApiModelProperty

class CertificateResponse {

    @ApiModelProperty(value = StackModelDescription.CERTIFICATE)
    var certificate: ByteArray? = null

    constructor() {

    }

    constructor(certificate: ByteArray) {
        this.certificate = certificate
    }
}
