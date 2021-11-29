package com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_CHANGED_CREDENTIAL;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_CHANGING_CREDENTIAL;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@Service
public class ClusterCredentialChangeService {
    @Inject
    private ClusterService clusterService;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    public void credentialChange(Long stackId) {
        flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_CHANGING_CREDENTIAL);
    }

    public void finishCredentialReplace(Long stackId, Long clusterId, String user, String password) {
        Cluster cluster = clusterService.getById(clusterId);
        cluster.setUserName(user);
        cluster.setPassword(password);
        finishCredentialChange(stackId, cluster);
    }

    public void finishCredentialUpdate(Long stackId, Long clusterId, String password) {
        Cluster cluster = clusterService.getById(clusterId);
        cluster.setPassword(password);
        finishCredentialChange(stackId, cluster);
    }

    private void finishCredentialChange(Long stackId, Cluster cluster) {
        clusterService.updateCluster(cluster);
        clusterService.updateClusterStatusByStackId(stackId, DetailedStackStatus.AVAILABLE);
        flowMessageService.fireEventAndLog(stackId, Status.AVAILABLE.name(), CLUSTER_CHANGED_CREDENTIAL);
    }
}
