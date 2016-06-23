package com.sequenceiq.cloudbreak.service.eventbus

import java.util.HashMap

import javax.annotation.PostConstruct
import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.data.repository.CrudRepository
import org.springframework.util.CollectionUtils

import com.sequenceiq.cloudbreak.cloud.service.Persister
import com.sequenceiq.cloudbreak.repository.EntityType

abstract class AbstractCloudPersisterService<T> : Persister<T> {

    @Inject
    private val repositoryList: List<CrudRepository<Any, Serializable>>? = null

    @Inject
    @Qualifier("conversionService")
    protected val conversionService: ConversionService? = null

    private val repositoryMap = HashMap<Class<Any>, CrudRepository<Any, Serializable>>()

    abstract override fun persist(data: T): T

    abstract override fun update(data: T): T

    abstract override fun retrieve(data: T): T

    @PostConstruct
    fun checkRepoMap() {
        if (CollectionUtils.isEmpty(repositoryList)) {
            throw IllegalStateException("No repositories provided!")
        } else {
            fillRepositoryMap()
        }
    }

    private fun fillRepositoryMap() {
        for (repo in repositoryList!!) {
            repositoryMap.put(getEntityClassForRepository(repo), repo)
        }
    }

    private fun getEntityClassForRepository(repo: CrudRepository<Any, Serializable>): Class<Any> {
        val originalInterface = repo.javaClass.getInterfaces()[0]
        val annotation = originalInterface.getAnnotation(EntityType::class.java) ?: throw IllegalStateException("Entity class is not specified for repository: " + originalInterface.getSimpleName())
        return annotation!!.entityClass()
    }

    protected fun <T> getRepositoryForEntity(clazz: Class<Any>): T {
        val repo = repositoryMap[clazz] as T ?: throw IllegalStateException("No repository found for the entityClass:" + clazz)
        return repo
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(AbstractCloudPersisterService<Any>::class.java)
    }
}
