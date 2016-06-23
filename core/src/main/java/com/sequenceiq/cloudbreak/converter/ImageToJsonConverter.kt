package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.ImageJson
import com.sequenceiq.cloudbreak.cloud.model.Image

@Component
class ImageToJsonConverter : AbstractConversionServiceAwareConverter<Image, ImageJson>() {

    override fun convert(source: Image): ImageJson {
        val imageJson = ImageJson()
        imageJson.imageName = source.imageName
        imageJson.hdpVersion = source.hdpVersion
        return imageJson
    }

}
