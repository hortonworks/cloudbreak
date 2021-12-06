package com.sequenceiq.cloudbreak.service.stack.archive;

import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.converter.InstanceMetadataToArchivedInstanceMetadataConverter;
import com.sequenceiq.cloudbreak.domain.stack.instance.ArchivedInstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.job.instancemetadata.ArchiveInstanceMetaDataConfig;
import com.sequenceiq.cloudbreak.repository.ArchivedInstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.FlowRetryService;

@Service
public class ArchiveInstanceMetaDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveInstanceMetaDataService.class);

    private static final int ZERO_PAGE_INDEX = 0;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ArchivedInstanceMetaDataRepository archivedInstanceMetaDataRepository;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private FlowRetryService flowRetryService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private ArchiveInstanceMetaDataConfig archiveInstanceMetaDataConfig;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private InstanceMetadataToArchivedInstanceMetadataConverter converter;

    public void archive(StackView stack) throws ArchiveInstanceMetaDataException {
        if (flowLogService.isOtherFlowRunning(stack.getId())) {
            String message = String.format("Another flow is running for stack %s, skipping archiving terminated InstanceMetaData to let the flow finish",
                    stack.getResourceCrn());
            throw new ArchiveInstanceMetaDataException(message);
        } else {
            Optional<FlowLog> lastRetryableFailedFlow = flowRetryService.getLastRetryableFailedFlow(stack.getId());
            if (lastRetryableFailedFlow.isEmpty()) {
                try {
                    LOGGER.info("Starting to archive terminated InstanceMetaData on stack {}", stack.getResourceCrn());
                    checkedMeasure(() -> doArchive(stack), LOGGER, "Archiving terminated InstanceMetaData took {} ms for stack {}.",
                            stack.getResourceCrn());
                    LOGGER.info("InstanceMetaData archivation finished successfully for stack {}", stack.getResourceCrn());
                } catch (ArchiveInstanceMetaDataException e) {
                    throw e;
                } catch (Exception e) {
                    String message = String.format("Something unexpected went wrong with stack %s while archiving terminated InstanceMetaData",
                            stack.getResourceCrn());
                    throw new ArchiveInstanceMetaDataException(message, e);
                }
            } else {
                String message = String.format("Stack %s has a retryable failed flow, " +
                        "skipping archiving terminated InstanceMetaData to preserve possible retry", stack.getResourceCrn());
                throw new ArchiveInstanceMetaDataException(message);
            }
        }
    }

    private void doArchive(StackView stack) throws ArchiveInstanceMetaDataException {
        int archiveOlderThanWeeks = archiveInstanceMetaDataConfig.getArchiveOlderThanWeeks();
        long thresholdTerminationDate = LocalDateTime.now().minusWeeks(archiveOlderThanWeeks).toInstant(ZoneOffset.UTC).toEpochMilli();

        int pageSize = archiveInstanceMetaDataConfig.getPageSize();
        PageRequest firstPageRequest = PageRequest.of(ZERO_PAGE_INDEX, pageSize);
        int totalPages = archiveInstanceMetaDataPage(stack, thresholdTerminationDate, firstPageRequest);
        for (int i = 1; i < totalPages; i++) {
            PageRequest pageRequest = PageRequest.of(i, pageSize);
            archiveInstanceMetaDataPage(stack, thresholdTerminationDate, pageRequest);
        }
    }

    private int archiveInstanceMetaDataPage(StackView stack, long thresholdTerminationDate, PageRequest pageRequest) throws ArchiveInstanceMetaDataException {
        int pageNumber = pageRequest.getPageNumber();
        String stackResourceCrn = stack.getResourceCrn();
        try {
            return transactionService.required(() -> {
                LOGGER.debug("Get batch #{} of terminated InstanceMetaData, that is oldar than {} week(s) (in epoch ms: {} ms) for stack {}",
                        pageNumber, archiveInstanceMetaDataConfig.getArchiveOlderThanWeeks(), thresholdTerminationDate, stackResourceCrn);
                Page<InstanceMetaData> page = instanceMetaDataService.getTerminatedInstanceMetaDataBefore(stack.getId(), thresholdTerminationDate, pageRequest);
                if (0 == page.getTotalElements()) {
                    LOGGER.debug("No InstanceMetaData with older termination date that {} found for stack: {}", thresholdTerminationDate, stackResourceCrn);
                    return page.getTotalPages();
                }
                LOGGER.debug("Total number of InstanceMetaData to archive is {} for stack {}", page.getTotalElements(), stackResourceCrn);

                List<ArchivedInstanceMetaData> archivedList = convertToArchived(page);
                LOGGER.debug("Save batch #{} of ArchivedInstanceMetaData.", pageNumber);
                archivedInstanceMetaDataRepository.saveAll(archivedList);
                LOGGER.debug("Saved batch #{} of ArchivedInstanceMetaData. Deleting original InstanceMetaData.", pageNumber);
                Set<InstanceGroup> instanceGroups = page.getContent().stream().map(InstanceMetaData::getInstanceGroup).collect(Collectors.toSet());
                instanceGroups.forEach(ig -> {
                    Set<Long> imIds = page.getContent().stream().map(InstanceMetaData::getId).collect(Collectors.toSet());
                    LOGGER.debug("Removed InstanceMetaData ids in this batch: {}", imIds);
                    ig.replaceInstanceMetadata(ig.getAllInstanceMetaData().stream().filter(im -> !imIds.contains(im.getId())).collect(Collectors.toSet()));
                });
                instanceGroupService.saveAll(instanceGroups);
                LOGGER.debug("Removed batch #{} of the original InstanceMetaData from respective InstanceGroups.", pageNumber);
                instanceMetaDataService.deleteAll(page.getContent());
                LOGGER.debug("Deleted batch #{} of the original InstanceMetaData.", pageNumber);

                return page.getTotalPages();
            });
        } catch (TransactionService.TransactionExecutionException e) {
            String message = String.format("Failed to archive the batch #%d of terminated instancemetadata for stack %s", pageNumber, stackResourceCrn);
            LOGGER.error(message);
            throw new ArchiveInstanceMetaDataException(message, e);
        }
    }

    private List<ArchivedInstanceMetaData> convertToArchived(Page<InstanceMetaData> page) {
        LOGGER.trace("Converting {} InstanceMetaData to ArchivedInstanceMetaData", page.getNumberOfElements());
        return page.stream()
                .map(converter::convert)
                .collect(Collectors.toList());
    }
}
