package com.sequenceiq.cloudbreak.service.image

import javax.ws.rs.client.Client
import javax.ws.rs.client.WebTarget

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.client.RestClientUtil
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakImageCatalog

@Service
class ImageCatalogProvider {

    @Value("${cb.image.catalog.url:}")
    private val catalogUrl: String? = null

    val imageCatalog: CloudbreakImageCatalog?
        get() {
            if (catalogUrl == null) {
                return null
            }
            try {
                if (catalogUrl.startsWith("http")) {
                    val client = RestClientUtil.get()
                    val target = client.target(catalogUrl)
                    return target.request().get().readEntity<CloudbreakImageCatalog>(CloudbreakImageCatalog::class.java)
                } else {
                    LOGGER.warn("Image catalog URL is not valid: {}", catalogUrl)
                }
            } catch (e: Exception) {
                LOGGER.warn("Failed to get image catalog", e)
            }

            return null
        }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ImageService::class.java)
    }

}
