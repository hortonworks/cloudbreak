package com.sequenceiq.cloudbreak.job.archive;

import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.util.NullUtil;

@Service
public class ArchiveClusterComponentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveClusterComponentService.class);

    private final ArchiveClusterComponentConfig archiveClusterComponentConfig;

    private final ClusterComponentConfigProvider clusterComponentConfigProvider;

    private final TransactionService transactionService;

    public ArchiveClusterComponentService(ArchiveClusterComponentConfig archiveClusterComponentConfig,
            ClusterComponentConfigProvider clusterComponentConfigProvider, TransactionService transactionService) {
        this.archiveClusterComponentConfig = archiveClusterComponentConfig;
        this.clusterComponentConfigProvider = clusterComponentConfigProvider;
        this.transactionService = transactionService;
    }

    public void archive(StackView stack) throws ArchiveClusterComponentException {
        NullUtil.throwIfNull(stack, () -> new IllegalArgumentException("Unable to archive ClusterComponent(s) for stack since the passed Stack object is " +
                "null!"));
        if (stack.getClusterView() != null) {
            try {
                LOGGER.info("Starting to archive ClusterComponent(s) for the terminated stack: {}", stack.getResourceCrn());
                checkedMeasure(() -> execute(stack), LOGGER, "Archiving ClusterComponent(s) took {} for terminated cluster: {}",
                        stack.getClusterView().getName());
                LOGGER.info("Archiving ClusterComponent(s) finished successfully for cluster {}", stack.getClusterView().getName());
            } catch (Exception e) {
                String message = String.format("Something unexpected went wrong with cluster %s while archiving ClusterComponent(s)",
                        stack.getClusterView().getName());
                throw new ArchiveClusterComponentException(message, e);
            }
        }
        LOGGER.debug("The given stack ({}) does not have cluster(view), therefore archiving ClusterComponent(s) is not executable", stack.getResourceCrn());
    }

    private void execute(StackView stackView) throws ArchiveClusterComponentException {
        int archiveOlderThanWeeks = archiveClusterComponentConfig.getArchiveOlderThanWeeks();
        long thresholdTerminationDate = LocalDateTime.now().minusWeeks(archiveOlderThanWeeks).toInstant(ZoneOffset.UTC).toEpochMilli();

        int pageSize = archiveClusterComponentConfig.getPageSize();
        PageRequest firstPageRequest = PageRequest.of(0, pageSize);
        int totalPages = archivePage(stackView, thresholdTerminationDate, firstPageRequest);
        for (int i = 1; i < totalPages; i++) {
            PageRequest pageRequest = PageRequest.of(i, pageSize);
            archivePage(stackView, thresholdTerminationDate, pageRequest);
        }
    }

    private int archivePage(StackView stack, long thresholdTerminationDate, PageRequest pageRequest) throws ArchiveClusterComponentException {
        int pageNumber = pageRequest.getPageNumber();
        String stackResourceCrn = stack.getResourceCrn();

        try {
            return transactionService.required(() -> {
                return -1;
            });
        } catch (TransactionService.TransactionExecutionException transactionExecutionException) {
            String message = String.format("Failed to archive the batch #%d of ClusterComponent(s) for cluster %s", pageNumber, stack.getClusterView().getName());
            LOGGER.error(message);
            throw new ArchiveClusterComponentException(message, transactionExecutionException);
        }

    }

}
