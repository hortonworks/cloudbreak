package com.sequenceiq.cloudbreak.shell.commands

import java.io.File

interface PlatformCommands {

    fun create(name: String, description: String, cloudPlatform: String, mapping: Map<String, String>): String

    fun convertMappingFile(file: File, url: String): Map<String, String>

    fun createPlatformAvailable(platform: String): Boolean
}
