package com.sequenceiq.cloudbreak.service.image

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties("cb")
@Component
class UserDataBuilderParams {

    var customData = "touch /tmp/cb-custom-data-default.txt"

}
