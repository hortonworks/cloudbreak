package com.sequenceiq.cloudbreak.service.cluster

import com.sequenceiq.cloudbreak.domain.Container
import com.sequenceiq.cloudbreak.repository.ContainerRepository
import org.springframework.stereotype.Service

import javax.inject.Inject
import javax.transaction.Transactional

@Service
@Transactional
class ContainerService {

    @Inject
    private val containerRepository: ContainerRepository? = null

    @Transactional(javax.transaction.Transactional.TxType.NEVER)
    fun save(containers: List<Container>): Iterable<Container> {
        return containerRepository!!.save(containers)
    }

    @Transactional(javax.transaction.Transactional.TxType.NEVER)
    fun findContainersInCluster(clusterId: Long?): Set<Container> {
        return containerRepository!!.findContainersInCluster(clusterId)
    }
}
