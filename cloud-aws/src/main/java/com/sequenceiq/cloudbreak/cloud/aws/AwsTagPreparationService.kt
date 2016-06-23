package com.sequenceiq.cloudbreak.cloud.aws

import java.util.ArrayList
import java.util.HashMap
import java.util.stream.Collectors

import javax.annotation.PostConstruct

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import com.amazonaws.services.cloudformation.model.Tag
import com.google.common.base.Strings
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext

@Service
class AwsTagPreparationService {

    @Value("${cb.aws.default.cf.tag:}")
    private val defaultCloudformationTag: String? = null

    @Value("#{'${cb.aws.custom.cf.tags:}'.split(',')}")
    private val customCloudformationTags: List<String>? = null

    private var customTags: MutableMap<String, String> = HashMap()

    @PostConstruct
    fun init() {
        customTags = HashMap<String, String>()
        if (customCloudformationTags != null && customCloudformationTags.size != 0) {
            customCloudformationTags.stream().filter({ field -> !field.isEmpty() }).forEach({ field ->
                val splittedField = field.split(":".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                customTags.put(splittedField[0], splittedField[1])
            })
        }
    }

    fun prepareTags(ac: AuthenticatedContext): Collection<Tag> {
        val tags = ArrayList<com.amazonaws.services.cloudformation.model.Tag>()
        tags.add(prepareTag(CLOUDBREAK_CLUSTER_TAG, ac.cloudContext.name))
        if (!Strings.isNullOrEmpty(defaultCloudformationTag)) {
            tags.add(prepareTag(CLOUDBREAK_ID, defaultCloudformationTag))
        }
        tags.addAll(customTags.entries.stream().map({ entry -> prepareTag(entry.key, entry.value) }).collect(Collectors.toList<Tag>()))
        return tags
    }

    private fun prepareTag(key: String, value: String): com.amazonaws.services.cloudformation.model.Tag {
        return com.amazonaws.services.cloudformation.model.Tag().withKey(key).withValue(value)
    }

    companion object {

        private val CLOUDBREAK_ID = "CloudbreakId"
        private val CLOUDBREAK_CLUSTER_TAG = "CloudbreakClusterName"
    }

}
