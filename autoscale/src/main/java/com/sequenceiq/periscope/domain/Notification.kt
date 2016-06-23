package com.sequenceiq.periscope.domain

import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.SequenceGenerator

@Entity
class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_generator")
    @SequenceGenerator(name = "notification_generator", sequenceName = "sequence_table")
    var id: Long = 0

    var target: Array<String>? = null

    @Enumerated(EnumType.STRING)
    var type: NotificationType? = null
}
