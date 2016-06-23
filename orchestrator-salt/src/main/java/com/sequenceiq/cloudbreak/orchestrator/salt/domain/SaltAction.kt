package com.sequenceiq.cloudbreak.orchestrator.salt.domain

import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltActionType

import java.util.ArrayList

class SaltAction(val action: SaltActionType) {

    var server: String? = null

    var minions: MutableList<Minion>? = null
        get() = minions

    fun addMinion(minion: Minion) {
        if (minions == null) {
            minions = ArrayList<Minion>()
        }
        minions!!.add(minion)
    }
}
