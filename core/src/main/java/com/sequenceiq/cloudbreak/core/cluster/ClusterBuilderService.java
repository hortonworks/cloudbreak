package com.sequenceiq.cloudbreak.core.cluster;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackType;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class ClusterBuilderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterBuilderService.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private DatalakeConfigProvider datalakeConfigProvider;

    @Inject
    private TransactionService transactionService;

    public void startCluster(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getByIdWithTransaction(stackId);
        ClusterApi connector = clusterApiConnectors.getConnector(stack.getCluster().getVariant());
        connector.waitForServer(stack);
        connector.changeOriginalCredentialsAndCreateCloudbreakUser(stack);
    }

    public void buildAmbariCluster(Long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        ClusterApi connector = clusterApiConnectors.getConnector(stack.getCluster().getVariant());
        connector.buildCluster(stack);
        if (StackType.DATALAKE == stack.getType()) {
            try {
                transactionService.required(() -> {
                    Stack stackInTransaction = stackService.getByIdWithListsInTransaction(stackId);
                    datalakeConfigProvider.collectAndStoreDatalakeResources(stackInTransaction);
                    return null;
                });
            } catch (TransactionService.TransactionExecutionException e) {
                LOGGER.info("Couldn't collect Datalake paramaters", e);
            }
        }
    }
}
