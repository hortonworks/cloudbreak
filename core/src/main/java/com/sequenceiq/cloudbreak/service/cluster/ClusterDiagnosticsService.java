package com.sequenceiq.cloudbreak.service.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.common.model.diagnostics.CmDiagnosticsParameters;

@Service
public class ClusterDiagnosticsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDiagnosticsService.class);

    @Inject
    private StackService stackService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private StatusToPollGroupConverter statusToPollGroupConverter;

    public void collectDiagnostics(Long stackId, CmDiagnosticsParameters parameters) throws CloudbreakException {
        LOGGER.debug("Called diagnostics collection ....");
        try {
            Stack stack = transactionService.required(() -> stackService.getByIdWithListsInTransaction(stackId));
            InMemoryStateStore.putStack(stack.getId(), statusToPollGroupConverter.convert(stack.getStatus()));
            if (stack.getCluster() != null) {
                InMemoryStateStore.putCluster(stack.getCluster().getId(), statusToPollGroupConverter.convert(stack.getStatus()));
            }
            clusterApiConnectors.getConnector(stack).clusterDiagnosticsService().collectDiagnostics(parameters);
        } catch (TransactionService.TransactionExecutionException e) {
            throw e.getCause();
        }
    }

    public List<String> getClusterComponents(String stackCrn) {
        List<String> components = new ArrayList<>();
        Stack stack = stackService.getByCrn(stackCrn);
        if (stack != null) {
            Cluster cluster = stack.getCluster();
            if (cluster != null && cluster.getBlueprint() != null) {
                CmTemplateProcessor cmTemplateProcessor = cmTemplateProcessorFactory.get(stack.getBlueprintJsonText());
                components = cmTemplateProcessor.getAllComponents()
                        .stream()
                        .map(ServiceComponent::getComponent)
                        .collect(Collectors.toCollection(ArrayList::new));
            }
        }
        return components;
    }
}
