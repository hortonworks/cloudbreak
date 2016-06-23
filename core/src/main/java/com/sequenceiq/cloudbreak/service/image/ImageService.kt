package com.sequenceiq.cloudbreak.service.image

import com.sequenceiq.cloudbreak.cloud.model.Platform.platform

import java.io.IOException

import javax.inject.Inject
import javax.transaction.Transactional

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.fasterxml.jackson.core.JsonProcessingException
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType
import com.sequenceiq.cloudbreak.cloud.PlatformParameters
import com.sequenceiq.cloudbreak.cloud.model.HDPInfo
import com.sequenceiq.cloudbreak.cloud.model.Image
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.common.type.ComponentType
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException
import com.sequenceiq.cloudbreak.domain.Component
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.domain.json.Json
import com.sequenceiq.cloudbreak.repository.ComponentRepository
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException
import com.sequenceiq.cloudbreak.service.TlsSecurityService

@Service
@Transactional
class ImageService {

    @Inject
    private val imageNameUtil: ImageNameUtil? = null

    @Inject
    private val hdpInfoSearchService: HdpInfoSearchService? = null

    @Inject
    private val userDataBuilder: UserDataBuilder? = null

    @Inject
    private val componentRepository: ComponentRepository? = null

    @Inject
    private val tlsSecurityService: TlsSecurityService? = null

    @Throws(CloudbreakImageNotFoundException::class)
    fun getImage(stackId: Long?): Image {
        try {
            val component = componentRepository!!.findComponentByStackIdComponentTypeName(stackId, ComponentType.IMAGE, IMAGE_NAME) ?: throw CloudbreakImageNotFoundException(String.format("Image not found: stackId: %d, componentType: %s, name: %s",
                    stackId, ComponentType.IMAGE.name, IMAGE_NAME))
            LOGGER.debug("Image found! stackId: {}, component: {}", stackId, component)
            return component.attributes.get<Image>(Image::class.java)
        } catch (e: IOException) {
            throw CloudbreakServiceException("Failed to read image", e)
        }

    }

    @Transactional(Transactional.TxType.NEVER)
    fun create(stack: Stack, params: PlatformParameters, ambariVersion: String, hdpVersion: String) {
        try {
            val platform = Companion.platform(stack.cloudPlatform())
            val platformString = Companion.platform(stack.cloudPlatform()).value().toLowerCase()
            var imageName = imageNameUtil!!.determineImageName(platformString, stack.region, ambariVersion, hdpVersion)
            val tmpSshKey = tlsSecurityService!!.readPublicSshKey(stack.id)
            val sshUser = stack.credential.loginUserName
            val publicSssKey = stack.credential.publicKey
            val userData = userDataBuilder!!.buildUserData(platform, publicSssKey, tmpSshKey, sshUser, params,
                    if (stack.relocateDocker == null) false else stack.relocateDocker)
            var hdpInfo: HDPInfo? = hdpInfoSearchService!!.searchHDPInfo(ambariVersion, hdpVersion)
            if (hdpInfo != null) {
                val specificImage = imageNameUtil.determineImageName(hdpInfo, platformString, stack.region)
                if (specificImage == null) {
                    LOGGER.warn("Cannot find image in the catalog, fallback to default image, ambari: {}, hdp: {}", ambariVersion, hdpVersion)
                    hdpInfo = null
                } else {
                    LOGGER.info("Determined image from catalog: {}", specificImage)
                    imageName = specificImage
                }
            }
            val image: Image
            if (hdpInfo == null) {
                image = Image(imageName, userData, null, null)
            } else {
                image = Image(imageName, userData, hdpInfo.repo, hdpInfo.version)
            }
            val component = Component(ComponentType.IMAGE, IMAGE_NAME, Json(image), stack)
            componentRepository!!.save(component)
            LOGGER.debug("Image saved: stackId: {}, component: {}", stack.id, component)
        } catch (e: JsonProcessingException) {
            throw CloudbreakServiceException("Failed to create json", e)
        } catch (e: CloudbreakSecuritySetupException) {
            throw CloudbreakServiceException("Failed to read temporary ssh credentials", e)
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ImageService::class.java)

        private val IMAGE_NAME = "IMAGE"
    }


}
