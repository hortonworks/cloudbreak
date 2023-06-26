package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory.ADMIN_USER;
import static java.util.Collections.singletonMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.FreeIpaErrorCodes;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.config.FreeIpaConfigService;
import com.sequenceiq.freeipa.service.freeipa.config.FreeIpaConfigView;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaNodeUtilService;

@Service
public class AdminUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminUserService.class);

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private FreeIpaConfigService freeIpaConfigService;

    @Inject
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    public void updateAdminUserPassword(String password, FreeIpaClient freeIpaClient) {
        try {
            LOGGER.info("Update password for admin user on FreeIPA");
            Integer minPwdLife = freeIpaClient.getPasswordPolicy().getKrbminpwdlife();
            if (minPwdLife != 0) {
                freeIpaClient.updatePasswordPolicy(Map.of("krbminpwdlife", 0L));
            }
            freeIpaClient.userSetPasswordWithExpiration(ADMIN_USER, password, Optional.empty());
        } catch (FreeIpaClientException e) {
            if (!FreeIpaClientExceptionUtil.isExceptionWithErrorCode(e, Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY))) {
                LOGGER.error("Failed to update admin password");
                throw new CloudbreakRuntimeException("Failed to update admin password", e);
            }
        }
    }

    public void updateFreeIpaPillar(Stack stack) {
        stack.getPrimaryGateway()
                .ifPresent(pgwInstanceMetadata -> {
                    Map<String, SaltPillarProperties> freeIpaPillar = new HashMap<>();
                    Set<InstanceMetaData> instanceMetaDatas = stack.getNotDeletedInstanceMetaDataSet();
                    Set<Node> allNodes = freeIpaNodeUtilService.mapInstancesToNodes(instanceMetaDatas);
                    FreeIpaConfigView freeIpaConfigView = freeIpaConfigService.createFreeIpaConfigs(stack, allNodes);
                    freeIpaPillar.put("freeipa", new SaltPillarProperties("/freeipa/init.sls", singletonMap("freeipa", freeIpaConfigView.toMap())));
                    OrchestratorStateParams stateParams = new OrchestratorStateParams();
                    stateParams.setTargetHostNames(allNodes.stream().map(Node::getHostname).collect(Collectors.toSet()));
                    GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, pgwInstanceMetadata);
                    stateParams.setPrimaryGatewayConfig(gatewayConfig);
                    try {
                        hostOrchestrator.saveCustomPillars(new SaltConfig(freeIpaPillar), null, stateParams);
                    } catch (CloudbreakOrchestratorFailedException e) {
                        LOGGER.error("Can't save freeipa pillar", e);
                        throw new CloudbreakRuntimeException(e);
                    }
                });
    }
}
