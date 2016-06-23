package com.sequenceiq.cloudbreak.domain

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.SequenceGenerator

@Entity
@NamedQueries(@NamedQuery(
        name = "Container.findContainersInCluster",
        query = "SELECT c FROM Container c "
                + "WHERE c.cluster.id= :clusterId"))
class Container {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "container_generator")
    @SequenceGenerator(name = "container_generator", sequenceName = "container_id_seq", allocationSize = 1)
    var id: Long? = null

    var containerId: String? = null

    var name: String? = null

    var image: String? = null

    var host: String? = null

    @ManyToOne
    var cluster: Cluster? = null

}
