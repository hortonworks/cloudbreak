package com.sequenceiq.cloudbreak.converter

import com.sequenceiq.cloudbreak.api.model.Status.REQUESTED

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson
import com.sequenceiq.cloudbreak.api.model.ClusterRequest
import com.sequenceiq.cloudbreak.api.model.FileSystemBase
import com.sequenceiq.cloudbreak.api.model.RDSConfigJson
import com.sequenceiq.cloudbreak.domain.AmbariStackDetails
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.FileSystem
import com.sequenceiq.cloudbreak.domain.RDSConfig

@Component
class JsonToClusterConverter : AbstractConversionServiceAwareConverter<ClusterRequest, Cluster>() {
    override fun convert(source: ClusterRequest): Cluster {
        val cluster = Cluster()
        cluster.name = source.name
        cluster.status = REQUESTED
        cluster.description = source.description
        cluster.emailNeeded = source.emailNeeded
        cluster.userName = source.userName
        cluster.password = source.password
        val enableSecurity = source.enableSecurity
        cluster.secure = enableSecurity ?: false
        cluster.kerberosMasterKey = source.kerberosMasterKey
        cluster.kerberosAdmin = source.kerberosAdmin
        cluster.kerberosPassword = source.kerberosPassword
        cluster.isLdapRequired = source.ldapRequired
        cluster.configStrategy = source.configStrategy
        val ambariStackDetails = source.ambariStackDetails
        cluster.enableShipyard = source.enableShipyard
        if (ambariStackDetails != null) {
            cluster.ambariStackDetails = conversionService.convert<AmbariStackDetails>(ambariStackDetails, AmbariStackDetails::class.java)
        }
        val rdsConfigJson = source.rdsConfigJson
        if (rdsConfigJson != null) {
            cluster.rdsConfig = conversionService.convert<RDSConfig>(rdsConfigJson, RDSConfig::class.java)
        }
        val fileSystem = source.fileSystem
        if (fileSystem != null) {
            cluster.fileSystem = conversionService.convert<FileSystem>(fileSystem, FileSystem::class.java)
        }
        return cluster
    }
}
