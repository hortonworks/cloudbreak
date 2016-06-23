package com.sequenceiq.cloudbreak.shell.commands

interface NetworkCommands {

    fun create(name: String, subnet: String, publicInAccount: Boolean?, description: String, platformId: Long?, parameters: Map<String, Any>, platform: String): String

    fun createNetworkAvailable(platform: String): Boolean
}
