package com.sequenceiq.periscope.rest.json

import com.sequenceiq.periscope.api.model.Json
import com.sequenceiq.periscope.domain.NotificationType

class NotificationJson : Json {

    var target: Array<String>? = null
    var notificationType: NotificationType? = null
}
