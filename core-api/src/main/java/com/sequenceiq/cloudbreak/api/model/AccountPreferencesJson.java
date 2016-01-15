package com.sequenceiq.cloudbreak.api.model;

import java.util.List;

import javax.validation.constraints.Digits;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.AccountPreferencesModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("AccountPreferences")
public class AccountPreferencesJson implements JsonEntity {

    @NotNull
    @Min(value = 0, message = "The maximum number of clusters has to be greater than '-1'")
    @Digits(fraction = 0, integer = 10, message = "The maximum number of clusters has to be a number")
    @ApiModelProperty(AccountPreferencesModelDescription.MAX_NO_CLUSTERS)
    private Long maxNumberOfClusters;

    @NotNull
    @Min(value = 0, message = "The maximum number of vms per cluster has to be greater than '-1'")
    @Digits(fraction = 0, integer = 10, message = "The maximum number of vms per cluster has to be a number")
    @ApiModelProperty(AccountPreferencesModelDescription.MAX_NO_NODES_PER_CLUSTER)
    private Long maxNumberOfNodesPerCluster;

    @NotNull
    @Min(value = 0, message = "The maximum number of clusters per user has to be greater than '-1'")
    @Digits(fraction = 0, integer = 10, message = "The maximum number of clusters per user has to be a number")
    @ApiModelProperty(AccountPreferencesModelDescription.MAX_NO_CLUSTERS_PER_USER)
    private Long maxNumberOfClustersPerUser;

    @ApiModelProperty(AccountPreferencesModelDescription.ALLOWED_INSTANCE_TYPES)
    private List<String> allowedInstanceTypes;

    @NotNull
    @Min(value = 0, message = "The cluster time to live has to be greater than '-1'")
    @ApiModelProperty(AccountPreferencesModelDescription.CLUSTER_TIME_TO_LIVE)
    private Long clusterTimeToLive;

    @NotNull
    @Min(value = 0, message = "The account time to live has to be greater than '-1'")
    @ApiModelProperty(AccountPreferencesModelDescription.ACCOUNT_TIME_TO_LIVE)
    private Long userTimeToLive;

    @ApiModelProperty(AccountPreferencesModelDescription.PLATFORMS)
    private String platforms;

    public Long getMaxNumberOfClusters() {
        return maxNumberOfClusters;
    }

    public void setMaxNumberOfClusters(Long maxNumberOfClusters) {
        this.maxNumberOfClusters = maxNumberOfClusters;
    }

    public Long getMaxNumberOfNodesPerCluster() {
        return maxNumberOfNodesPerCluster;
    }

    public void setMaxNumberOfNodesPerCluster(Long maxNumberOfNodesPerCluster) {
        this.maxNumberOfNodesPerCluster = maxNumberOfNodesPerCluster;
    }

    public List<String> getAllowedInstanceTypes() {
        return allowedInstanceTypes;
    }

    public void setAllowedInstanceTypes(List<String> allowedInstanceTypes) {
        this.allowedInstanceTypes = allowedInstanceTypes;
    }

    public Long getClusterTimeToLive() {
        return clusterTimeToLive;
    }

    public void setClusterTimeToLive(Long clusterTimeToLive) {
        this.clusterTimeToLive = clusterTimeToLive;
    }

    public Long getUserTimeToLive() {
        return userTimeToLive;
    }

    public void setUserTimeToLive(Long userTimeToLive) {
        this.userTimeToLive = userTimeToLive;
    }

    public Long getMaxNumberOfClustersPerUser() {
        return maxNumberOfClustersPerUser;
    }

    public void setMaxNumberOfClustersPerUser(Long maxNumberOfClustersPerUser) {
        this.maxNumberOfClustersPerUser = maxNumberOfClustersPerUser;
    }

    public String getPlatforms() {
        return platforms;
    }

    public void setPlatforms(String platforms) {
        this.platforms = platforms;
    }
}
