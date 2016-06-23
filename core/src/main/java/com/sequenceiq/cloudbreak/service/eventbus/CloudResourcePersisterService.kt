package com.sequenceiq.cloudbreak.service.eventbus

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotification
import com.sequenceiq.cloudbreak.domain.Resource
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.repository.ResourceRepository
import com.sequenceiq.cloudbreak.repository.StackRepository

@Component
class CloudResourcePersisterService : AbstractCloudPersisterService<ResourceNotification>() {

    override fun persist(notification: ResourceNotification): ResourceNotification {
        LOGGER.debug("Resource allocation notification received: {}", notification)
        val stackId = notification.cloudContext.id
        val cloudResource = notification.cloudResource
        val resource = conversionService.convert<Resource>(cloudResource, Resource::class.java)
        resource.stack = stackRepository.findByIdLazy(stackId)
        resourceRepository.save(resource)
        return notification
    }

    override fun update(notification: ResourceNotification): ResourceNotification {
        LOGGER.debug("Resource update notification received: {}", notification)
        val stackId = notification.cloudContext.id
        val cloudResource = notification.cloudResource
        val repository = resourceRepository
        val persistedResource = repository.findByStackIdAndNameAndType(stackId, cloudResource.name, cloudResource.type)
        val resource = conversionService.convert<Resource>(cloudResource, Resource::class.java)
        updateWithPersistedFields(resource, persistedResource)
        resource.stack = stackRepository.findByIdLazy(stackId)
        repository.save(resource)
        return notification
    }

    override fun delete(notification: ResourceNotification): ResourceNotification {
        LOGGER.debug("Resource deletion notification received: {}", notification)
        val stackId = notification.cloudContext.id
        val cloudResource = notification.cloudResource
        val repository = resourceRepository
        val resource = repository.findByStackIdAndNameAndType(stackId, cloudResource.name, cloudResource.type)
        if (resource != null) {
            repository.delete(resource)
        }
        return notification
    }

    override fun retrieve(data: ResourceNotification): ResourceNotification? {
        return null
    }

    private val stackRepository: StackRepository
        get() = getRepositoryForEntity(Stack::class.java)

    private val resourceRepository: ResourceRepository
        get() = getRepositoryForEntity(Resource::class.java)

    private fun updateWithPersistedFields(resource: Resource, persistedResource: Resource?): Resource {
        if (persistedResource != null) {
            resource.id = persistedResource.id
            resource.instanceGroup = persistedResource.instanceGroup
        }
        return resource
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(CloudResourcePersisterService::class.java)
    }
}
