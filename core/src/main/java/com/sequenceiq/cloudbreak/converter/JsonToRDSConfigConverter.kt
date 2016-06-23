package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.RDSConfigJson
import com.sequenceiq.cloudbreak.domain.RDSConfig

@Component
class JsonToRDSConfigConverter : AbstractConversionServiceAwareConverter<RDSConfigJson, RDSConfig>() {
    override fun convert(source: RDSConfigJson): RDSConfig {
        val rdsConfig = RDSConfig()
        rdsConfig.connectionURL = source.connectionURL
        rdsConfig.connectionUserName = source.connectionUserName
        rdsConfig.connectionPassword = source.connectionPassword
        rdsConfig.databaseType = source.databaseType
        return rdsConfig
    }
}