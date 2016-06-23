package com.sequenceiq.cloudbreak.shell.commands

import java.io.File

interface CredentialCommands {

    fun create(name: String, sshKeyPath: File, sshKeyUrl: String, sshKeyString: String,
               description: String, publicInAccount: Boolean?, platformId: Long?, parameters: Map<String, Any>, platform: String): String

    fun createCredentialAvailable(platform: String): Boolean
}
