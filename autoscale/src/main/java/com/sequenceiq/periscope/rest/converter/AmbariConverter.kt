package com.sequenceiq.periscope.rest.converter

import org.springframework.stereotype.Component

import com.sequenceiq.periscope.domain.Ambari
import com.sequenceiq.periscope.api.model.AmbariJson

@Component
class AmbariConverter : AbstractConverter<AmbariJson, Ambari>() {

    override fun convert(source: AmbariJson): Ambari {
        return Ambari(source.host, source.port, source.user, source.pass)
    }

}
