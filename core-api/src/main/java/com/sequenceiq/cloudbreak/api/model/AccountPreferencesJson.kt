package com.sequenceiq.cloudbreak.api.model

import javax.validation.constraints.Digits
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.AccountPreferencesModelDescription

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("AccountPreferences")
class AccountPreferencesJson : JsonEntity {

    @NotNull
    @Min(value = 0, message = "The maximum number of clusters has to be greater than '-1'")
    @Digits(fraction = 0, integer = 10, message = "The maximum number of clusters has to be a number")
    @ApiModelProperty(AccountPreferencesModelDescription.MAX_NO_CLUSTERS)
    var maxNumberOfClusters: Long? = null

    @NotNull
    @Min(value = 0, message = "The maximum number of vms per cluster has to be greater than '-1'")
    @Digits(fraction = 0, integer = 10, message = "The maximum number of vms per cluster has to be a number")
    @ApiModelProperty(AccountPreferencesModelDescription.MAX_NO_NODES_PER_CLUSTER)
    var maxNumberOfNodesPerCluster: Long? = null

    @NotNull
    @Min(value = 0, message = "The maximum number of clusters per user has to be greater than '-1'")
    @Digits(fraction = 0, integer = 10, message = "The maximum number of clusters per user has to be a number")
    @ApiModelProperty(AccountPreferencesModelDescription.MAX_NO_CLUSTERS_PER_USER)
    var maxNumberOfClustersPerUser: Long? = null

    @ApiModelProperty(AccountPreferencesModelDescription.ALLOWED_INSTANCE_TYPES)
    var allowedInstanceTypes: List<String>? = null

    @NotNull
    @Min(value = 0, message = "The cluster time to live has to be greater than '-1'")
    @ApiModelProperty(AccountPreferencesModelDescription.CLUSTER_TIME_TO_LIVE)
    var clusterTimeToLive: Long? = null

    @NotNull
    @Min(value = 0, message = "The account time to live has to be greater than '-1'")
    @ApiModelProperty(AccountPreferencesModelDescription.ACCOUNT_TIME_TO_LIVE)
    var userTimeToLive: Long? = null

    @ApiModelProperty(AccountPreferencesModelDescription.PLATFORMS)
    var platforms: String? = null
}
