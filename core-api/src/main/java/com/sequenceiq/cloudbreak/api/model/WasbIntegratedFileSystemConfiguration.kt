package com.sequenceiq.cloudbreak.api.model

import javax.validation.constraints.NotNull

class WasbIntegratedFileSystemConfiguration : FileSystemConfiguration() {

    @NotNull
    var tenantId: String? = null
    @NotNull
    var subscriptionId: String? = null
    @NotNull
    var appId: String? = null
    @NotNull
    var appPassword: String? = null
    @NotNull
    var region: String? = null
    @NotNull
    var storageName: String? = null
}
