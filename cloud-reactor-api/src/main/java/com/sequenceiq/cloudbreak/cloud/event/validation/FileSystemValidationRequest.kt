package com.sequenceiq.cloudbreak.cloud.event.validation

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.model.FileSystem

class FileSystemValidationRequest(val fileSystem: FileSystem, cloudContext: CloudContext) : CloudPlatformRequest<FileSystemValidationResult>(cloudContext, null) {

    override fun toString(): String {
        val sb = StringBuilder("FileSystemValidationRequest{")
        sb.append("fileSystem=").append(fileSystem)
        sb.append('}')
        return sb.toString()
    }
}
