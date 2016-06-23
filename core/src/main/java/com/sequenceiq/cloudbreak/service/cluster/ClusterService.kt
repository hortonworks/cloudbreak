package com.sequenceiq.cloudbreak.service.cluster

import com.sequenceiq.cloudbreak.api.model.ClusterResponse
import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson
import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.api.model.StatusRequest
import com.sequenceiq.cloudbreak.api.model.UserNamePasswordJson
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException
import com.sequenceiq.cloudbreak.domain.AmbariStackDetails
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.service.stack.flow.HttpClientConfig

interface ClusterService {

    fun create(user: CbUser, stackId: Long?, clusterRequest: Cluster): Cluster

    fun delete(user: CbUser, stackId: Long?)

    fun retrieveClusterByStackId(stackId: Long?): Cluster

    fun retrieveClusterForCurrentUser(stackId: Long?): ClusterResponse

    fun updateAmbariClientConfig(clusterId: Long?, ambariClientConfig: HttpClientConfig): Cluster

    fun updateHostCountWithAdjustment(clusterId: Long?, hostGroupName: String, adjustment: Int?)

    fun updateHostMetadata(clusterId: Long?, hostsPerHostGroup: Map<String, List<String>>)

    fun getClusterJson(ambariIp: String, stackId: Long?): String

    @Throws(CloudbreakSecuritySetupException::class)
    fun updateHosts(stackId: Long?, hostGroupAdjustment: HostGroupAdjustmentJson)

    fun updateStatus(stackId: Long?, statusRequest: StatusRequest)

    fun updateClusterStatusByStackId(stackId: Long?, status: Status, statusReason: String): Cluster

    fun updateClusterStatusByStackId(stackId: Long?, status: Status): Cluster

    fun updateCluster(cluster: Cluster): Cluster

    fun updateClusterMetadata(stackId: Long?): Cluster

    fun updateClusterUsernameAndPassword(cluster: Cluster, userName: String, password: String): Cluster

    fun recreate(stackId: Long?, blueprintId: Long?, hostGroups: Set<HostGroup>, validateBlueprint: Boolean, ambariStackDetails: AmbariStackDetails): Cluster

    fun updateUserNamePassword(stackId: Long?, userNamePasswordJson: UserNamePasswordJson): Cluster

    fun getClusterResponse(response: ClusterResponse, clusterJson: String): ClusterResponse

    fun getById(clusterId: Long?): Cluster
}
