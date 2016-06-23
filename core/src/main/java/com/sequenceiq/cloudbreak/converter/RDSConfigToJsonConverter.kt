package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.RDSConfigJson
import com.sequenceiq.cloudbreak.domain.RDSConfig

@Component
class RDSConfigToJsonConverter : AbstractConversionServiceAwareConverter<RDSConfig, RDSConfigJson>() {
    override fun convert(source: RDSConfig): RDSConfigJson {
        val json = RDSConfigJson()
        json.connectionURL = source.connectionURL
        json.connectionUserName = source.connectionUserName
        json.connectionPassword = source.connectionPassword
        json.databaseType = source.databaseType
        return json
    }
}
