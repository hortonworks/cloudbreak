package com.sequenceiq.cloudbreak.reactor.handler.cluster.certrotate;

import java.security.KeyPair;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterCertificatesRotationFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterHostCertificatesRotationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterHostCertificatesRotationSuccess;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.ssh.SshKeyService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class ClusterHostCertificatesRotationHandler extends ExceptionCatcherEventHandler<ClusterHostCertificatesRotationRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterHostCertificatesRotationHandler.class);

    private static final String ROOT_USER = "root";

    private static final String TEMPORARY_SSH_KEY = "temporary ssh key for CM host cert rotation";

    @Inject
    private StackService stackService;

    @Inject
    private ClusterApiConnectors apiConnectors;

    @Inject
    private SshKeyService sshKeyService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterHostCertificatesRotationRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterHostCertificatesRotationRequest> event) {
        return new ClusterCertificatesRotationFailed(resourceId, e);
    }

    protected Selectable doAccept(HandlerEvent<ClusterHostCertificatesRotationRequest> event) {
        LOGGER.debug("Accepting Cluster Manager host certificates rotation request...");
        ClusterHostCertificatesRotationRequest request = event.getData();
        Selectable result;
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
            ClusterApi clusterApi = apiConnectors.getConnector(stack);
            if (isRootSshAccessNeededForHostCertRotation(stack)) {
                rotateCertsWithSsh(stack, clusterApi);
            } else {
                clusterApi.rotateHostCertificates(null, null);
            }
            result = new ClusterHostCertificatesRotationSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.info("Cluster Manager host certificates rotation failed", e);
            result = new ClusterCertificatesRotationFailed(request.getResourceId(), e);
        }
        return result;
    }

    private boolean isRootSshAccessNeededForHostCertRotation(Stack stack) {
        return CMRepositoryVersionUtil.isRootSshAccessNeededForHostCertRotation(
                clusterComponentConfigProvider.getClouderaManagerRepoDetails(stack.getCluster().getId()));
    }

    private void rotateCertsWithSsh(Stack stack, ClusterApi clusterApi) throws Exception {
        KeyPair sshKeyPair = sshKeyService.generateKeyPair();
        sshKeyService.addSshPublicKeyToHosts(stack, ROOT_USER, sshKeyPair, TEMPORARY_SSH_KEY);
        clusterApi.rotateHostCertificates(ROOT_USER, sshKeyPair);
        sshKeyService.removeSshPublicKeyFromHosts(stack, ROOT_USER, TEMPORARY_SSH_KEY);
    }
}
