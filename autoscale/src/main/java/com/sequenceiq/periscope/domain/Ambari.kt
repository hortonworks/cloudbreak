package com.sequenceiq.periscope.domain

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.SequenceGenerator

@Entity
class Ambari {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ambari_generator")
    @SequenceGenerator(name = "ambari_generator", sequenceName = "sequence_table")
    var id: Long = 0

    @Column(name = "ambari_host")
    var host: String? = null

    @Column(name = "ambari_port")
    var port: String? = null

    @Column(name = "ambari_user")
    var user: String? = null

    @Column(name = "ambari_pass")
    var pass: String? = null

    constructor() {
    }

    constructor(host: String, port: String, user: String, pass: String) {
        this.host = host
        this.port = port
        this.user = user
        this.pass = pass
    }
}
