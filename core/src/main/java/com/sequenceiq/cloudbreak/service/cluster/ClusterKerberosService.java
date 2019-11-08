package com.sequenceiq.cloudbreak.service.cluster;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class ClusterKerberosService {
    @Inject
    private HostOrchestratorResolver hostOrchestratorResolver;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private KerberosDetailService kerberosDetailService;

    public void leaveDomains(Stack stack, boolean forced) throws CloudbreakException {
        leaveDomains(stack, stackUtil.collectNodes(stack), forced);
    }

    public void leaveDomains(Stack stack, Set<Node> nodes, boolean forced) throws CloudbreakException {
        KerberosConfig kerberosConfig = kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName()).orElse(null);
        if (kerberosDetailService.isAdJoinable(kerberosConfig) || kerberosDetailService.isIpaJoinable(kerberosConfig)) {
            try {
                HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
                GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
                ExitCriteriaModel noExitModel = ClusterDeletionBasedExitCriteriaModel.nonCancellableModel();
                if (kerberosDetailService.isAdJoinable(kerberosConfig)) {
                    hostOrchestrator.leaveDomain(gatewayConfig, nodes, "ad_member", "ad_leave", noExitModel, forced);
                } else if (kerberosDetailService.isIpaJoinable(kerberosConfig)) {
                    hostOrchestrator.leaveDomain(gatewayConfig, nodes, "ipa_member", "ipa_leave", noExitModel, forced);
                }
            } catch (CloudbreakOrchestratorFailedException e) {
                Set<Map.Entry<String, Collection<String>>> entries = e.getNodesWithErrors().asMap().entrySet();
                String errors;
                errors = entries.isEmpty() ? e.getMessage() : entries.stream()
                        .map(entry -> entry.getKey() + ": " + entry.getValue())
                        .collect(Collectors.joining("\n"));
                String message = "Leaving AD domain had some errors:\n" + errors;
                throw new CloudbreakException(message, e);
            }
        }
    }
}
