package com.sequenceiq.cloudbreak.api.model


import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern

import io.swagger.annotations.ApiModelProperty

class SubscriptionRequest {

    @NotNull
    @Pattern(regexp = SIMPLE_URL_PATTERN, message = "The notification hook URL must be proper and valid!")
    @ApiModelProperty(required = true)
    var endpointUrl: String? = null

    companion object {

        internal val SIMPLE_URL_PATTERN = "^(https?:\\/\\/)((([\\da-z\\.-]+)\\.([a-z]{2,6}))|localhost|[1-9][0-9]{0,2}.[0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3})" + "(:[1-9][0-9]{1,4})?\\/([\\/\\w\\.-]*)\\/?$"
    }
}
