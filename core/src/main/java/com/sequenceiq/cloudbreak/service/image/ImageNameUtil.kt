package com.sequenceiq.cloudbreak.service.image

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.model.HDPInfo

@Component
class ImageNameUtil {

    @Inject
    private val environment: Environment? = null

    fun determineImageName(platform: String, region: String, ambariVersion: String?, hdpVersion: String): String {
        var image = getDefaultImage(platform, region)
        if (ambariVersion != null) {
            val specificImage = getSpecificImage(platform, region, ambariVersion, hdpVersion)
            if (specificImage != null) {
                image = specificImage
            } else {
                LOGGER.info("The specified ambari-hdp version image not found: ambari: {} hdp: ", ambariVersion, hdpVersion)
            }
        }
        LOGGER.info("Selected VM image for CloudPlatform '{}' is: {}", platform, image)
        return image
    }

    fun determineImageName(hdpInfo: HDPInfo, platform: String, region: String): String? {
        val regions = hdpInfo.images!![platform]
        if (regions != null) {
            val image = regions[region]
            return image ?: regions[DEFAULT]
        }
        return null
    }

    private fun getDefaultImage(platform: String, region: String): String {
        var image: String? = getImage(platform + "." + region)
        if (image == null) {
            image = getImage(platform + "." + DEFAULT)
        }
        return image
    }

    private fun getSpecificImage(platform: String, region: String, ambariVersion: String, hdpVersion: String): String? {
        var image: String? = getImage(String.format("%s-ambari_%s-hdp_%s.%s", platform, ambariVersion, hdpVersion, region))
        if (image == null) {
            image = getImage(String.format("%s-ambari_%s-hdp_%s.%s", platform, ambariVersion, hdpVersion, DEFAULT))
        }
        return image
    }


    private fun getImage(key: String): String {
        return environment!!.getProperty(key)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ImageNameUtil::class.java)
        private val DEFAULT = "default"
    }
}
