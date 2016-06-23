package com.sequenceiq.cloudbreak.domain

import java.util.ArrayList

import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.SequenceGenerator

@Entity
@NamedQueries(@NamedQuery(
        name = "Topology.findAllInAccount",
        query = "SELECT t FROM Topology t "
                + "WHERE t.account= :account AND deleted IS NOT TRUE "), @NamedQuery(
        name = "Topology.findByIdInAccount",
        query = "SELECT t FROM Topology t "
                + "WHERE t.id= :id and t.account= :account AND deleted IS NOT TRUE "))
class Topology {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "topology_generator")
    @SequenceGenerator(name = "topology_generator", sequenceName = "topology_id_seq", allocationSize = 1)
    var id: Long? = null
    @Column(nullable = false)
    var name: String? = null
    @Column(length = 1000000, columnDefinition = "TEXT")
    var description: String? = null
    @Column(nullable = false)
    var cloudPlatform: String? = null
    @Column(nullable = false)
    var account: String? = null
    @Column(nullable = false)
    var owner: String? = null

    var isDeleted: Boolean = false

    @ElementCollection(fetch = FetchType.EAGER)
    var records: List<TopologyRecord> = ArrayList()
}
