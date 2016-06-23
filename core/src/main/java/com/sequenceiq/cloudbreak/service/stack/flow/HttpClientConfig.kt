package com.sequenceiq.cloudbreak.service.stack.flow

class HttpClientConfig {

    var apiAddress: String? = null
        private set
    var apiPort: Int? = null
        private set
    var serverCert: String? = null
        private set
    var clientCert: String? = null
        private set
    var clientKey: String? = null
        private set

    constructor(apiAddress: String, apiPort: Int?) {
        this.apiAddress = apiAddress
        this.apiPort = apiPort
    }

    constructor(apiAddress: String, apiPort: Int?, certDir: String?) {
        this.apiAddress = apiAddress
        this.apiPort = apiPort
        if (certDir != null) {
            this.serverCert = certDir + DEFAULT_SERVER_CERT_NAME
            this.clientCert = certDir + DEFAULT_CLIENT_CERT_NAME
            this.clientKey = certDir + DEFAULT_PRIVATE_KEY_NAME
        }
    }

    val certDir: String?
        get() {
            if (serverCert != null) {
                return serverCert!!.substring(0, serverCert!!.indexOf(DEFAULT_SERVER_CERT_NAME))
            }
            return null
        }

    companion object {
        private val DEFAULT_PRIVATE_KEY_NAME = "/key.pem"
        private val DEFAULT_CLIENT_CERT_NAME = "/cert.pem"
        private val DEFAULT_SERVER_CERT_NAME = "/ca.pem"
    }
}