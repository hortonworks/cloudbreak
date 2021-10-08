package com.sequenceiq.cloudbreak.core.flow2.stack.detach;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationFailedException;

/**
 * This class detaches the stack from the environment.
 * <p>
 * It is achieved by renaming name and stack CRN. CLuster associated with the stack is also renamed as well.
 * Before this operation is performed, the data lake associated with this stack is also renamed so that DL name and stack same are the same.
 * <p>
 */
@Service
public class StackUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackUpdateService.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackService stackService;

    @Inject
    private TransactionService transactionService;

    public void updateNameAndCrn(Stack stack, String newName, String newCrn) {
        MDCBuilder.buildMdcContext(stack);
        try {
            transactionService.required(() -> {
                stack.setName(newName);
                stack.setResourceCrn(newCrn);
                Stack savedStack = measure(() -> stackService.save(stack),
                        LOGGER, "Stack save took {} ms for stack {}", stack.getName());
                // While detaching the stack, name of the cluster is renamed to avoid conflicts.
                Optional<Cluster> cluster = clusterService.findOneByStackId(savedStack.getId());
                cluster.map(c -> {
                    c.setName(newName);
                    return c;
                }).map(clusterService::save);
            });
        } catch (TransactionService.TransactionExecutionException ex) {
            LOGGER.info("Failed to update the stack. Stack id {}", stack.getId());
            throw new TerminationFailedException(ex);
        }
    }
}
