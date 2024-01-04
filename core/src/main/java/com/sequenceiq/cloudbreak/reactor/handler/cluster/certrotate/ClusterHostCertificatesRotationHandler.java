package com.sequenceiq.cloudbreak.reactor.handler.cluster.certrotate;

import static com.google.common.base.Preconditions.checkNotNull;

import java.security.KeyPair;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterCertificatesRotationFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterHostCertificatesRotationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterHostCertificatesRotationSuccess;
import com.sequenceiq.cloudbreak.san.LoadBalancerSANProvider;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.ssh.SshKeyService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterHostCertificatesRotationHandler extends ExceptionCatcherEventHandler<ClusterHostCertificatesRotationRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterHostCertificatesRotationHandler.class);

    private static final String ROOT_USER = "root";

    private static final String TEMPORARY_SSH_KEY = "temporary ssh key for CM host cert rotation";

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterApiConnectors apiConnectors;

    @Inject
    private SshKeyService sshKeyService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private LoadBalancerSANProvider loadBalancerSANProvider;

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
            StackDto stackDto = stackDtoService.getById(request.getResourceId());
            checkNotNull(stackDto);
            checkNotNull(stackDto.getStack());
            checkNotNull(stackDto.getCluster());
            ClusterApi clusterApi = apiConnectors.getConnector(stackDto);
            StackView stack = stackDto.getStack();
            String subAltName = loadBalancerSANProvider.getLoadBalancerSAN(stack.getId(), stackDto.getBlueprint()).orElse(null);
            if (isRootSshAccessNeededForHostCertRotation(stackDto)) {
                rotateCertsWithSsh(stackDto, clusterApi, subAltName);
            } else {
                clusterApi.rotateHostCertificates(null, null, subAltName);
            }
            result = new ClusterHostCertificatesRotationSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.info("Cluster Manager host certificates rotation failed", e);
            result = new ClusterCertificatesRotationFailed(request.getResourceId(), e);
        }
        return result;
    }

    private boolean isRootSshAccessNeededForHostCertRotation(StackDto stackDto) {
        return CMRepositoryVersionUtil.isRootSshAccessNeededForHostCertRotation(
                clusterComponentConfigProvider.getClouderaManagerRepoDetails(stackDto.getCluster().getId()));
    }

    private void rotateCertsWithSsh(StackDto stackDto, ClusterApi clusterApi, String subAltName) throws Exception {
        KeyPair sshKeyPair = sshKeyService.generateKeyPair();
        sshKeyService.addSshPublicKeyToHosts(stackDto, ROOT_USER, sshKeyPair, TEMPORARY_SSH_KEY);
        clusterApi.rotateHostCertificates(ROOT_USER, sshKeyPair, subAltName);
        sshKeyService.removeSshPublicKeyFromHosts(stackDto, ROOT_USER, TEMPORARY_SSH_KEY);
    }
}
