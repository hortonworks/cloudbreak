package com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@Service
public class ClusterCredentialChangeService {
    @Inject
    private ClusterService clusterService;

    @Inject
    private FlowMessageService flowMessageService;

    public void credentialChange(Long stackId) {
        flowMessageService.fireEventAndLog(stackId, Msg.AMBARI_CLUSTER_CHANGING_CREDENTIAL, Status.UPDATE_IN_PROGRESS.name());
    }

    public void finishCredentialChange(Long stackId, Cluster cluster, String user, String password) {
        clusterService.updateClusterUsernameAndPassword(cluster, user, password);
        clusterService.updateClusterStatusByStackId(stackId, Status.AVAILABLE);
        flowMessageService.fireEventAndLog(stackId, Msg.AMBARI_CLUSTER_CHANGED_CREDENTIAL, Status.AVAILABLE.name());
    }

}
