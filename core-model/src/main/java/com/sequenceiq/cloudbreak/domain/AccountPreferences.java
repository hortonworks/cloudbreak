package com.sequenceiq.cloudbreak.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;

@Entity
@Table(name = "account_preferences")
@NamedQueries({
        @NamedQuery(
                name = "AccountPreferences.findByAccount",
                query = "SELECT ap FROM AccountPreferences ap "
                        + "WHERE ap.account= :account")
})
public class AccountPreferences {
    private static final String INSTANCE_TYPE_SEPARATOR = ",";

    @Id
    private String account;
    private Long maxNumberOfClusters;
    private Long maxNumberOfNodesPerCluster;
    private Long maxNumberOfClustersPerUser;
    private String allowedInstanceTypes;
    private Long clusterTimeToLive;
    private Long userTimeToLive;
    @Column(columnDefinition = "TEXT")
    private String platforms;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

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
        return StringUtils.isEmpty(allowedInstanceTypes) ? new ArrayList<String>() : Arrays.asList(allowedInstanceTypes.split(INSTANCE_TYPE_SEPARATOR));
    }

    public void setAllowedInstanceTypes(Iterable<String> allowedInstanceTypes) {
        StringBuilder builder = new StringBuilder();
        Iterator<String> it = allowedInstanceTypes.iterator();
        while (it.hasNext()) {
            String instanceType = it.next();
            builder.append(instanceType);
            if (it.hasNext()) {
                builder.append(INSTANCE_TYPE_SEPARATOR);
            }
        }
        this.allowedInstanceTypes = builder.toString();
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
