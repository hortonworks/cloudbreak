package com.sequenceiq.cloudbreak.service.cluster.flow.filesystem

import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration
import com.sequenceiq.cloudbreak.api.model.FileSystemType
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.BlueprintConfigurationEntry
import com.sequenceiq.cloudbreak.service.cluster.flow.RecipeScript

interface FileSystemConfigurator<T : FileSystemConfiguration> {

    fun createResources(fsConfig: T): Map<String, String>

    fun deleteResources(fsConfig: T): Map<String, String>

    fun getFsProperties(fsConfig: T, resourceProperties: Map<String, String>): List<BlueprintConfigurationEntry>

    fun getDefaultFsValue(fsConfig: T): String

    fun getDefaultFsProperties(fsConfig: T): List<BlueprintConfigurationEntry>

    fun getScripts(fsConfig: T): List<RecipeScript>

    val fileSystemType: FileSystemType

}
