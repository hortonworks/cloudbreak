package com.sequenceiq.cloudbreak.shell.commands


interface TemplateCommands {

    fun create(name: String, instanceType: String, volumeCount: Int?, volumeSize: Int?, volumeType: String, publicInAccount: Boolean?, description: String,
               parameters: Map<String, Any>, platformId: Long?, platform: String): String

    fun createTemplateAvailable(platform: String): Boolean
}
