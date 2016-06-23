package com.sequenceiq.cloudbreak.converter.spi

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.FileSystemRequest
import com.sequenceiq.cloudbreak.cloud.model.FileSystem
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter

@Component
class FileSystemRequestToFileSystemConverter : AbstractConversionServiceAwareConverter<FileSystemRequest, FileSystem>() {
    override fun convert(source: FileSystemRequest): FileSystem {
        return FileSystem(source.name, source.type.name, source.isDefaultFs, source.properties)
    }
}
