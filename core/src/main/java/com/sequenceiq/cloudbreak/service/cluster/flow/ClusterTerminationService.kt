package com.sequenceiq.cloudbreak.service.cluster.flow

import com.sequenceiq.cloudbreak.api.model.Status.DELETE_COMPLETED
import com.sequenceiq.cloudbreak.util.JsonUtil.readValue
import com.sequenceiq.cloudbreak.util.JsonUtil.writeValueAsString

import java.io.IOException
import java.util.Date
import java.util.HashMap
import java.util.HashSet
import java.util.LinkedList
import java.util.stream.Collectors

import javax.annotation.Resource
import javax.inject.Inject
import javax.transaction.Transactional

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration
import com.sequenceiq.cloudbreak.api.model.FileSystemType
import com.sequenceiq.cloudbreak.core.CloudbreakException
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.Constraint
import com.sequenceiq.cloudbreak.domain.Container
import com.sequenceiq.cloudbreak.domain.FileSystem
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.Orchestrator
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential
import com.sequenceiq.cloudbreak.repository.ClusterRepository
import com.sequenceiq.cloudbreak.repository.ConstraintRepository
import com.sequenceiq.cloudbreak.repository.ContainerRepository
import com.sequenceiq.cloudbreak.repository.HostGroupRepository
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository
import com.sequenceiq.cloudbreak.service.TlsSecurityService
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemConfigurator
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationFailedException

@Component
@Transactional
class ClusterTerminationService {

    @Inject
    private val clusterRepository: ClusterRepository? = null
    @Inject
    private val hostGroupRepository: HostGroupRepository? = null
    @Resource
    private val fileSystemConfigurators: Map<FileSystemType, FileSystemConfigurator<FileSystemConfiguration>>? = null
    @Inject
    private val hostMetadataRepository: HostMetadataRepository? = null
    @Inject
    private val constraintRepository: ConstraintRepository? = null
    @Inject
    private val containerRepository: ContainerRepository? = null
    @Inject
    private val containerOrchestratorResolver: ContainerOrchestratorResolver? = null
    @Inject
    private val tlsSecurityService: TlsSecurityService? = null

    fun deleteClusterContainers(clusterId: Long?) {
        val cluster = clusterRepository!!.findById(clusterId)
        if (cluster == null) {
            val msg = String.format("Failed to delete containers of cluster (id:'%s'), because the cluster could not be found in the database.", clusterId)
            throw TerminationFailedException(msg)
        }
        deleteClusterContainers(cluster)
    }

    fun deleteClusterContainers(cluster: Cluster) {
        try {
            val orchestrator = cluster.stack.orchestrator
            val map = HashMap<String, Any>()
            map.putAll(orchestrator.attributes.map)
            map.put("certificateDir", tlsSecurityService!!.prepareCertDir(cluster.stack.id))
            val credential = OrchestrationCredential(orchestrator.apiEndpoint, map)
            val containerOrchestrator = containerOrchestratorResolver!!.get(orchestrator.type)
            val containers = containerRepository!!.findContainersInCluster(cluster.id)
            val containerInfo = containers.stream().map({ c -> ContainerInfo(c.getContainerId(), c.getName(), c.getHost(), c.getImage()) }).collect(Collectors.toList<ContainerInfo>())
            containerOrchestrator.deleteContainer(containerInfo, credential)
            containerRepository.delete(containers)
            deleteClusterHostGroupsWithItsMetadata(cluster)
        } catch (e: CloudbreakException) {
            throw TerminationFailedException(String.format("Failed to delete containers of cluster (id:'%s',name:'%s').",
                    cluster.id, cluster.name), e)
        } catch (e: CloudbreakOrchestratorException) {
            throw TerminationFailedException(String.format("Failed to delete containers of cluster (id:'%s',name:'%s').", cluster.id, cluster.name), e)
        }

    }

    fun finalizeClusterTermination(clusterId: Long?) {
        val cluster = clusterRepository!!.findById(clusterId)
        val terminatedName = cluster.name + DELIMITER + Date().time
        cluster.name = terminatedName
        val fs = cluster.fileSystem
        if (fs != null) {
            deleteFileSystemResources(cluster.stack.id, fs)
        }
        cluster.blueprint = null
        cluster.stack = null
        cluster.sssdConfig = null
        cluster.status = DELETE_COMPLETED
        deleteClusterHostGroupsWithItsMetadata(cluster)
    }

    private fun deleteClusterHostGroupsWithItsMetadata(cluster: Cluster) {
        val hostGroups = HashSet(cluster.hostGroups)
        val constraintsToDelete = LinkedList<Constraint>()
        for (hg in hostGroups) {
            hg.recipes.clear()
            val constraint = hg.constraint
            if (constraint != null) {
                constraintsToDelete.add(constraint)
            }
        }
        hostGroupRepository!!.delete(hostGroups)
        constraintRepository!!.delete(constraintsToDelete)
        cluster.hostGroups.clear()
        cluster.containers.clear()
        clusterRepository!!.save(cluster)
    }

    private fun deleteFileSystemResources(stackId: Long?, fileSystem: FileSystem): Map<String, String> {
        try {
            val fsConfigurator = fileSystemConfigurators!![FileSystemType.valueOf(fileSystem.type)]
            val json = INSTANCE.writeValueAsString(fileSystem.properties)
            INSTANCE.readValue(json, FileSystemType.valueOf(fileSystem.type).clazz).addProperty(FileSystemConfiguration.STORAGE_CONTAINER, "cloudbreak" + stackId!!)
            return fsConfigurator.deleteResources(INSTANCE.readValue(json, FileSystemType.valueOf(fileSystem.type).clazz))
        } catch (e: IOException) {
            throw TerminationFailedException("File system resources could not be deleted: ", e)
        }

    }

    companion object {

        private val DELIMITER = "_"
    }
}
