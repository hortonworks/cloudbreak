package com.sequenceiq.periscope.domain

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.NamedQuery
import javax.persistence.Table

@Entity
@Table(name = "periscope_user")
@NamedQuery(name = "PeriscopeUser.findOneByName", query = "SELECT b FROM PeriscopeUser b " + "WHERE b.email= :email")
class PeriscopeUser {

    @Id
    var id: String? = null

    var email: String? = null

    var account: String? = null

    constructor() {
    }

    constructor(id: String, email: String, account: String) {
        this.id = id
        this.email = email
        this.account = account
    }
}
