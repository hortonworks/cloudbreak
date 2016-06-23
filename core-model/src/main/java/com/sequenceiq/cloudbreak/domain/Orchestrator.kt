package com.sequenceiq.cloudbreak.domain

import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.SequenceGenerator

import com.sequenceiq.cloudbreak.domain.json.Json
import com.sequenceiq.cloudbreak.domain.json.JsonToString

@Entity
class Orchestrator {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "orchestrator_generator")
    @SequenceGenerator(name = "orchestrator_generator", sequenceName = "orchestrator_id_seq", allocationSize = 1)
    var id: Long? = null

    var apiEndpoint: String? = null

    @Column(nullable = false)
    var type: String? = null

    @Convert(converter = JsonToString::class)
    @Column(columnDefinition = "TEXT")
    var attributes: Json? = null
}
