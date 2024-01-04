package com.sequenceiq.cloudbreak.ssh;

import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateRetryParams;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;

@Component
public class SshKeyService {
    private static final String REMOVE_SSH_PUBLICKEY_STATE = "ssh.remove_ssh_publickey";

    private static final String ADD_SSH_PUBLICKEY_STATE = "ssh.add_ssh_publickey";

    private static final int SSH_KEY_OPERATION_RETRY_COUNT = 10;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackUtil stackUtil;

    public KeyPair generateKeyPair() {
        return PkiUtil.generateKeypair();
    }

    public void addSshPublicKeyToHosts(StackDto stackDto, String user, KeyPair keyPair, String authKeysComment) throws Exception {
        OrchestratorStateParams stateParams = createSshStateParams(stackDto, user, keyPair, authKeysComment, REMOVE_SSH_PUBLICKEY_STATE);
        hostOrchestrator.runOrchestratorState(stateParams);
        stateParams.setState(ADD_SSH_PUBLICKEY_STATE);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    public void removeSshPublicKeyFromHosts(StackDto stackDto, String user, String authKeysComment) throws Exception {
        OrchestratorStateParams stateParams = createSshStateParams(stackDto, user, null, authKeysComment, REMOVE_SSH_PUBLICKEY_STATE);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    private OrchestratorStateParams createSshStateParams(StackDto stackDto, String user, KeyPair keyPair, String authKeysComment, String saltState) {
        StackView stack = stackDto.getStack();
        ClusterView cluster = stackDto.getCluster();
        Set<Node> nodes = stackUtil.collectReachableNodes(stackDto);
        OrchestratorStateParams stateParams = new OrchestratorStateParams();
        stateParams.setState(saltState);
        stateParams.setPrimaryGatewayConfig(gatewayConfigService.getGatewayConfig(stack, stackDto.getSecurityConfig(), stackDto.getPrimaryGatewayInstance(),
                stackDto.hasGateway()));
        stateParams.setTargetHostNames(nodes.stream().map(Node::getHostname).collect(Collectors.toSet()));
        stateParams.setExitCriteriaModel(ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel(stack.getId(), cluster.getId()));
        stateParams.setStateParams(createSshParams(user, keyPair, authKeysComment));
        OrchestratorStateRetryParams retryParams = new OrchestratorStateRetryParams();
        retryParams.setMaxRetry(SSH_KEY_OPERATION_RETRY_COUNT);
        stateParams.setStateRetryParams(retryParams);
        return stateParams;
    }

    private Map<String, Object> createSshParams(String user, KeyPair keyPair, String authKeysComment) {
        Map<String, String> sshParams = new HashMap<>();
        sshParams.put("user", user);
        sshParams.put("comment", authKeysComment);
        if (keyPair != null) {
            sshParams.put("publickey", PkiUtil.convertOpenSshPublicKey(keyPair.getPublic()));
        }
        return Map.of("tmpssh", sshParams);
    }
}
