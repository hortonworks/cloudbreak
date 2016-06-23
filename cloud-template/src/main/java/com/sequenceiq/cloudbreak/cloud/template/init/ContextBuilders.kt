package com.sequenceiq.cloudbreak.cloud.template.init

import java.util.HashMap

import javax.annotation.PostConstruct
import javax.inject.Inject

import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.template.ResourceContextBuilder

@Service
class ContextBuilders {

    @Inject
    private val contextBuilders: List<ResourceContextBuilder<ResourceBuilderContext>>? = null
    private val map = HashMap<Platform, ResourceContextBuilder<ResourceBuilderContext>>()

    @PostConstruct
    fun init() {
        for (builder in contextBuilders!!) {
            map.put(builder.platform(), builder)
        }
    }

    operator fun get(platform: Platform): ResourceContextBuilder<ResourceBuilderContext> {
        return map[platform]
    }

}
