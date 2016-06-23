package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.domain.AccountPreferences
import com.sequenceiq.cloudbreak.api.model.AccountPreferencesJson

@Component
class JsonToAccountPreferencesConverter : AbstractConversionServiceAwareConverter<AccountPreferencesJson, AccountPreferences>() {

    override fun convert(source: AccountPreferencesJson): AccountPreferences {
        val target = AccountPreferences()
        target.maxNumberOfClusters = source.maxNumberOfClusters
        target.maxNumberOfNodesPerCluster = source.maxNumberOfNodesPerCluster
        val allowedInstanceTypes = source.allowedInstanceTypes
        if (allowedInstanceTypes != null && !allowedInstanceTypes.isEmpty()) {
            target.setAllowedInstanceTypes(allowedInstanceTypes)
        }
        target.clusterTimeToLive = source.clusterTimeToLive!! * HOUR_IN_MS
        target.userTimeToLive = source.userTimeToLive!! * HOUR_IN_MS
        target.maxNumberOfClustersPerUser = source.maxNumberOfClustersPerUser
        target.platforms = source.platforms
        return target
    }

    companion object {
        private val HOUR_IN_MS = 3600000L
    }

}
