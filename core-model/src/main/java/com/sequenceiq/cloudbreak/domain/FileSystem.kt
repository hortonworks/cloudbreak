package com.sequenceiq.cloudbreak.domain

import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.MapKeyColumn
import javax.persistence.SequenceGenerator

@Entity
class FileSystem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "filesystem_generator")
    @SequenceGenerator(name = "filesystem_generator", sequenceName = "filesystem_id_seq", allocationSize = 1)
    var id: Long? = null

    var name: String? = null

    var type: String? = null

    var isDefaultFs: Boolean = false

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "key")
    @Column(name = "value", columnDefinition = "TEXT", length = 100000)
    var properties: Map<String, String>? = null

    constructor() {
    }

    constructor(name: String, type: String, defaultFs: Boolean) {
        this.name = name
        this.type = type
        this.isDefaultFs = defaultFs
    }
}
