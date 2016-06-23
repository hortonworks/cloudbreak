package com.sequenceiq.periscope.rest.converter

import org.springframework.stereotype.Component

import com.sequenceiq.periscope.domain.Notification
import com.sequenceiq.periscope.rest.json.NotificationJson

@Component
class NotificationConverter : AbstractConverter<NotificationJson, Notification>() {

    override fun convert(source: NotificationJson): Notification {
        val notification = Notification()
        notification.target = source.target
        notification.type = source.notificationType
        return notification
    }

    override fun convert(source: Notification): NotificationJson {
        val json = NotificationJson()
        json.notificationType = source.type
        json.target = source.target
        return json
    }
}
