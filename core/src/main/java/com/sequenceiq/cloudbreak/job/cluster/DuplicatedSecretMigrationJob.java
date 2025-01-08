package com.sequenceiq.cloudbreak.job.cluster;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.Optional;

import jakarta.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@DisallowConcurrentExecution
@Component
public class DuplicatedSecretMigrationJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(DuplicatedSecretMigrationJob.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private DuplicatedSecretMigrationJobService jobService;

    @Override
    protected Optional<Object> getMdcContextObject() {
        return Optional.ofNullable(stackDtoService.getByCrn(getRemoteResourceCrn()));
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        try {
            measure(() -> {
                LOGGER.info("Duplicated secrets were already migrated, unscheduling job for stack {}", getRemoteResourceCrn());
                jobService.unschedule(context.getJobDetail().getKey());
            }, LOGGER, "Migrate duplicated secrets took {} ms for cluster {}.", getRemoteResourceCrn());
        } catch (Exception e) {
            LOGGER.error(String.format("Failed to execute duplicated secret migration for stack %s", getRemoteResourceCrn()), e);
        }
    }
}
