package com.sequenceiq.cloudbreak.converter

import java.text.SimpleDateFormat

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.domain.CloudbreakUsage
import com.sequenceiq.cloudbreak.api.model.CloudbreakUsageJson
import com.sequenceiq.cloudbreak.service.user.UserDetailsService
import com.sequenceiq.cloudbreak.service.user.UserFilterField

@Component
class CloudbreakUsageToJsonConverter : AbstractConversionServiceAwareConverter<CloudbreakUsage, CloudbreakUsageJson>() {

    @Inject
    private val userDetailsService: UserDetailsService? = null

    override fun convert(entity: CloudbreakUsage): CloudbreakUsageJson {
        val json = CloudbreakUsageJson()
        val day = DATE_FORMAT.format(entity.day)
        var cbUser: String? = null
        try {
            cbUser = userDetailsService!!.getDetails(entity.owner, UserFilterField.USERID).username
        } catch (ex: Exception) {
            LOGGER.warn(String.format("Expected user was not found with '%s' id. Maybe it was deleted by the admin user.", entity.owner))
            cbUser = entity.owner
        }

        json.owner = entity.owner
        json.account = entity.account
        json.provider = entity.provider
        json.region = entity.region
        json.availabilityZone = entity.availabilityZone
        json.instanceHours = entity.instanceHours
        json.day = day
        json.stackId = entity.stackId
        json.stackName = entity.stackName
        json.username = cbUser
        json.costs = entity.costs
        json.instanceType = entity.instanceType
        json.instanceGroup = entity.instanceGroup
        return json
    }

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd")
        private val LOGGER = LoggerFactory.getLogger(CloudbreakUsageToJsonConverter::class.java)
    }

}
