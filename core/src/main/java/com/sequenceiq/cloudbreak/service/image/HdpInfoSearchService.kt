package com.sequenceiq.cloudbreak.service.image

import java.util.Collections
import java.util.stream.Collectors

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.model.AmbariCatalog
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakImageCatalog
import com.sequenceiq.cloudbreak.cloud.model.HDPInfo

@Service
class HdpInfoSearchService {

    @Inject
    private val imageCatalogProvider: ImageCatalogProvider? = null

    fun searchHDPInfo(ambariVersion: String?, hdpVersion: String?): HDPInfo {
        var hdpInfo: HDPInfo? = null
        if (ambariVersion != null && hdpVersion != null) {
            val imageCatalog = imageCatalogProvider!!.imageCatalog
            hdpInfo = prefixSearch(imageCatalog, ambariVersion, hdpVersion)
        }
        return hdpInfo
    }

    private fun ambariPrefixMatch(imageCatalog: CloudbreakImageCatalog?, ambariVersion: String): List<AmbariCatalog>? {
        if (imageCatalog == null) {
            return null
        }
        val ambariCatalogs = imageCatalog.ambariVersions!!.stream().filter({ p -> p.ambariInfo!!.version!!.startsWith(ambariVersion) }).collect(Collectors.toList<AmbariCatalog>())
        Collections.sort<AmbariCatalog>(ambariCatalogs, Collections.reverseOrder(VersionComparator()))
        LOGGER.info("Prefix matched Ambari versions: {}. Ambari search prefix: {}", ambariCatalogs, ambariVersion)
        return ambariCatalogs
    }

    private fun selectLatestAmbariCatalog(ambariCatalogs: List<AmbariCatalog>?): AmbariCatalog? {
        if (ambariCatalogs == null || ambariCatalogs.isEmpty()) {
            return null
        }
        return ambariCatalogs[0]
    }

    private fun hdpPrefixMatch(ambariCatalog: AmbariCatalog?, hdpVersion: String): List<HDPInfo>? {
        if (ambariCatalog == null) {
            return null
        }
        val hdpInfos = ambariCatalog.ambariInfo!!.hdp!!.stream().filter({ p -> p.version!!.startsWith(hdpVersion) }).collect(Collectors.toList<HDPInfo>())
        Collections.sort<HDPInfo>(hdpInfos, Collections.reverseOrder(VersionComparator()))
        LOGGER.info("Prefix matched HDP versions: {} for Ambari version: {}. HDP search prefix: {}", hdpInfos, ambariCatalog.version, hdpVersion)
        return hdpInfos

    }

    private fun selectLatestHdpInfo(hdpInfos: List<HDPInfo>?): HDPInfo? {
        if (hdpInfos == null || hdpInfos.isEmpty()) {
            return null
        }
        return hdpInfos[0]
    }

    private fun prefixSearch(imageCatalog: CloudbreakImageCatalog, ambariVersion: String, hdpVersion: String): HDPInfo {
        val ambariCatalogs = ambariPrefixMatch(imageCatalog, ambariVersion)
        val ambariCatalog = selectLatestAmbariCatalog(ambariCatalogs)
        val hdpInfos = hdpPrefixMatch(ambariCatalog, hdpVersion)
        return selectLatestHdpInfo(hdpInfos)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(HdpInfoSearchService::class.java)
    }

}
