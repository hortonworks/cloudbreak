package com.sequenceiq.cloudbreak.converter

import java.util.HashMap

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.FileSystemRequest
import com.sequenceiq.cloudbreak.domain.FileSystem

@Component
class JsonToFileSystemConverter : AbstractConversionServiceAwareConverter<FileSystemRequest, FileSystem>() {
    override fun convert(source: FileSystemRequest): FileSystem {
        val fs = FileSystem()
        fs.name = source.name
        fs.type = source.type.name
        fs.isDefaultFs = source.isDefaultFs
        if (source.properties != null) {
            fs.properties = source.properties
        } else {
            fs.properties = HashMap<String, String>()
        }
        return fs
    }
}
