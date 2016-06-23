package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.SssdConfigRequest
import com.sequenceiq.cloudbreak.domain.SssdConfig

@Component
class JsonToSssdConfigConverter : AbstractConversionServiceAwareConverter<SssdConfigRequest, SssdConfig>() {

    override fun convert(json: SssdConfigRequest): SssdConfig {
        val config = SssdConfig()
        config.name = json.name
        config.description = json.description
        config.providerType = json.providerType
        config.url = json.url
        config.schema = json.schema
        config.baseSearch = json.baseSearch
        config.tlsReqcert = json.tlsReqcert
        config.adServer = json.adServer
        config.kerberosServer = json.kerberosServer
        config.kerberosRealm = json.kerberosRealm
        config.configuration = json.configuration
        return config
    }
}
