package com.sequenceiq.cloudbreak.api.model

import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern

class WasbFileSystemConfiguration : FileSystemConfiguration() {
    @NotNull
    @Pattern(regexp = "^[a-z0-9]{3,24}$", message = "Must contain only numbers and lowercase letters and must be between 3 and 24 characters long.")
    var accountName: String? = null
    @NotNull
    @Pattern(regexp = "^([A-Za-z0-9+/]{4}){21}([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$", message = "Must be the base64 encoded representation of 64 random bytes.")
    var accountKey: String? = null
}
