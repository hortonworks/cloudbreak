package com.sequenceiq.cloudbreak.api.model

enum class FileSystemType private constructor(val clazz: Class<Any>) {
    DASH(DashFileSystemConfiguration::class.java),
    WASB_INTEGRATED(WasbIntegratedFileSystemConfiguration::class.java),
    GCS(GcsFileSystemConfiguration::class.java),
    WASB(WasbFileSystemConfiguration::class.java)
}
