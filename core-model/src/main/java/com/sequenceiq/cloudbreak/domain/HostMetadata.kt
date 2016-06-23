package com.sequenceiq.cloudbreak.domain

import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.SequenceGenerator

import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

import com.sequenceiq.cloudbreak.common.type.HostMetadataState

@Entity
@NamedQueries(@NamedQuery(
        name = "HostMetadata.findHostsInCluster",
        query = "SELECT h FROM HostMetadata h "
                + "WHERE h.hostGroup.cluster.id= :clusterId"), @NamedQuery(
        name = "HostMetadata.findHostInClusterByName",
        query = "SELECT h FROM HostMetadata h "
                + "WHERE h.hostGroup.cluster.id= :clusterId AND h.hostName = :hostName"), @NamedQuery(
        name = "HostMetadata.findEmptyContainerHostsInHostGroup",
        query = "SELECT h FROM HostMetadata h "
                + "WHERE h.hostGroup.id= :hostGroupId AND h.hostMetadataState= 'CONTAINER_RUNNING'"))
class HostMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "hostmetadata_generator")
    @SequenceGenerator(name = "hostmetadata_generator", sequenceName = "hostmetadata_id_seq", allocationSize = 1)
    var id: Long? = null

    var hostName: String? = null

    @ManyToOne
    var hostGroup: HostGroup? = null

    @Enumerated(EnumType.STRING)
    var hostMetadataState = HostMetadataState.HEALTHY

    override fun hashCode(): Int {
        return HashCodeBuilder.reflectionHashCode(this, "hostGroup")
    }

    override fun equals(obj: Any?): Boolean {
        return EqualsBuilder.reflectionEquals(this, obj, "hostGroup")
    }

}
