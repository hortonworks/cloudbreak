package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.SssdConfigResponse
import com.sequenceiq.cloudbreak.domain.SssdConfig

@Component
class SssdConfigToJsonConverter : AbstractConversionServiceAwareConverter<SssdConfig, SssdConfigResponse>() {
    override fun convert(config: SssdConfig): SssdConfigResponse {
        val json = SssdConfigResponse()
        json.name = config.name
        json.description = config.description
        json.providerType = config.providerType
        json.url = config.url
        json.schema = config.schema
        json.baseSearch = config.baseSearch
        json.tlsReqcert = config.tlsReqcert
        json.adServer = config.adServer
        json.kerberosServer = config.kerberosServer
        json.kerberosRealm = config.kerberosRealm
        json.configuration = config.configuration
        json.id = config.id
        json.isPublicInAccount = config.isPublicInAccount
        return json
    }
}
