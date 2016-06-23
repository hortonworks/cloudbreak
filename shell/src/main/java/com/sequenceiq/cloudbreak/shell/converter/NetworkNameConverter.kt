package com.sequenceiq.cloudbreak.shell.converter

import javax.inject.Inject

import org.springframework.shell.core.Completion
import org.springframework.shell.core.MethodTarget

import com.sequenceiq.cloudbreak.client.CloudbreakClient
import com.sequenceiq.cloudbreak.shell.completion.NetworkName
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer

class NetworkNameConverter : AbstractConverter<NetworkName>() {

    @Inject
    private val cloudbreakClient: CloudbreakClient? = null

    @Inject
    private val responseTransformer: ResponseTransformer<Collection<Any>>? = null

    override fun supports(type: Class<*>, optionContext: String): Boolean {
        return NetworkName::class.java!!.isAssignableFrom(type)
    }

    override fun getAllPossibleValues(completions: MutableList<Completion>, targetType: Class<*>, existingData: String, optionContext: String, target: MethodTarget): Boolean {
        try {
            val networksMap = responseTransformer!!.transformToMap(cloudbreakClient!!.networkEndpoint().publics, "id", "name")
            return getAllPossibleValues(completions, networksMap.values)
        } catch (e: Exception) {
            return false
        }

    }
}
