package com.sequenceiq.cloudbreak.api.model

import javax.validation.constraints.NotNull

class GcsFileSystemConfiguration : FileSystemConfiguration() {
    @NotNull
    var projectId: String? = null
    @NotNull
    var serviceAccountEmail: String? = null
    @NotNull
    var privateKeyEncoded: String? = null
    @NotNull
    var defaultBucketName: String? = null
}
