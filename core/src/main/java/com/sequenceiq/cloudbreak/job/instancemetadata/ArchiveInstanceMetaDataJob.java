package com.sequenceiq.cloudbreak.job.instancemetadata;

import javax.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
import com.sequenceiq.cloudbreak.service.stack.archive.ArchiveInstanceMetaDataException;
import com.sequenceiq.cloudbreak.service.stack.archive.ArchiveInstanceMetaDataService;

import io.opentracing.Tracer;

@DisallowConcurrentExecution
@Component
public class ArchiveInstanceMetaDataJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveInstanceMetaDataJob.class);

    @Inject
    private StackViewService stackViewService;

    @Inject
    private ArchiveInstanceMetaDataJobService jobService;

    @Inject
    private ArchiveInstanceMetaDataService archiveInstanceMetaDataService;

    public ArchiveInstanceMetaDataJob(Tracer tracer) {
        super(tracer, "Archive InstanceMetaData Job");
    }

    @Override
    protected Object getMdcContextObject() {
        return stackViewService.findById(getStackId()).orElseGet(StackView::new);
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        StackView stackView = stackViewService.findById(getStackId()).orElseGet(StackView::new);
        Status stackStatus = stackView.getStatus();
        if (!Status.getUnschedulableStatuses().contains(stackStatus)) {
            archiveInstanceMetaDataOnStack(stackView);
        } else {
            LOGGER.debug("Existing stack InstanceMetaData archiving will be descheduled, because stack {} state is {}", stackView.getResourceCrn(), stackStatus);
            jobService.unschedule(context.getJobDetail().getKey());
        }
    }

    private void archiveInstanceMetaDataOnStack(StackView stackView) throws JobExecutionException {
        try {
            archiveInstanceMetaDataService.archive(stackView);
        } catch (ArchiveInstanceMetaDataException e) {
            LOGGER.error("Failed to archive terminated InstanceMetaData for stack: {}", stackView.getResourceCrn(), e);
            throw new JobExecutionException(String.format("Failed to archive terminated InstanceMetaData for stack: %s, exception: %s",
                    stackView.getResourceCrn(), e));
        }
    }

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }
}
