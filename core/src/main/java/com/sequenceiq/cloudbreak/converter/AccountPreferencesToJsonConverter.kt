package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.domain.AccountPreferences
import com.sequenceiq.cloudbreak.api.model.AccountPreferencesJson


@Component
class AccountPreferencesToJsonConverter : AbstractConversionServiceAwareConverter<AccountPreferences, AccountPreferencesJson>() {

    override fun convert(source: AccountPreferences): AccountPreferencesJson {
        val json = AccountPreferencesJson()
        json.maxNumberOfClusters = source.maxNumberOfClusters
        json.maxNumberOfNodesPerCluster = source.maxNumberOfNodesPerCluster
        json.allowedInstanceTypes = source.allowedInstanceTypes
        val clusterTimeToLive = if (source.clusterTimeToLive === ZERO) ZERO else source.clusterTimeToLive!! / HOUR_IN_MS
        json.clusterTimeToLive = clusterTimeToLive
        val userTimeToLive = if (source.userTimeToLive === ZERO) ZERO else source.userTimeToLive!! / HOUR_IN_MS
        json.userTimeToLive = userTimeToLive
        json.maxNumberOfClustersPerUser = source.maxNumberOfClustersPerUser
        json.platforms = source.platforms
        return json
    }

    companion object {
        private val HOUR_IN_MS = 3600000L
        private val ZERO = 0L
    }
}
