package com.sequenceiq.cloudbreak.domain

import java.util.ArrayList
import java.util.Arrays

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.Table

import org.apache.commons.lang3.StringUtils

@Entity
@Table(name = "account_preferences")
@NamedQueries(@NamedQuery(
        name = "AccountPreferences.findByAccount",
        query = "SELECT ap FROM AccountPreferences ap "
                + "WHERE ap.account= :account"))
class AccountPreferences {

    @Id
    var account: String? = null
    var maxNumberOfClusters: Long? = null
    var maxNumberOfNodesPerCluster: Long? = null
    var maxNumberOfClustersPerUser: Long? = null
    private var allowedInstanceTypes: String? = null
    var clusterTimeToLive: Long? = null
    var userTimeToLive: Long? = null
    @Column(columnDefinition = "TEXT")
    var platforms: String? = null

    fun getAllowedInstanceTypes(): List<String> {
        return if (StringUtils.isEmpty(allowedInstanceTypes)) ArrayList<String>() else Arrays.asList<String>(*allowedInstanceTypes!!.split(INSTANCE_TYPE_SEPARATOR.toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray())
    }

    fun setAllowedInstanceTypes(allowedInstanceTypes: Iterable<String>) {
        val builder = StringBuilder()
        val it = allowedInstanceTypes.iterator()
        while (it.hasNext()) {
            val instanceType = it.next()
            builder.append(instanceType)
            if (it.hasNext()) {
                builder.append(INSTANCE_TYPE_SEPARATOR)
            }
        }
        this.allowedInstanceTypes = builder.toString()
    }

    companion object {
        private val INSTANCE_TYPE_SEPARATOR = ","
    }
}
