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

import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

import io.micrometer.common.util.StringUtils;

@DisallowConcurrentExecution
@Component
public class DuplicatedSecretMigrationJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(DuplicatedSecretMigrationJob.class);

    @Inject
    private ClusterService clusterService;

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
                StackDto stackDto = stackDtoService.getByCrn(getRemoteResourceCrn());
                if (stackDto.getStatus() == null || !stackDto.getStatus().isInProgress()) {
                    Cluster cluster = clusterService.getByIdWithLists(getLocalIdAsLong());
                    if (StringUtils.isEmpty(cluster.getCloudbreakClusterManagerUser()) || StringUtils.isEmpty(cluster.getDpClusterManagerUser())) {
                        cluster.setCloudbreakClusterManagerUserFromOld();
                        cluster.setCloudbreakClusterManagerPasswordFromOld();
                        cluster.setDpClusterManagerUserFromOld();
                        cluster.setDpClusterManagerPasswordFromOld();
                        clusterService.save(cluster);
                        LOGGER.info("Duplicated secrets are migrated for stack {}", getRemoteResourceCrn());
                    } else {
                        LOGGER.info("Duplicated secrets were already migrated, unscheduling job for stack {}", getRemoteResourceCrn());
                        jobService.unschedule(context.getJobDetail().getKey());
                    }
                }
            }, LOGGER, "Migrate duplicated secrets took {} ms for cluster {}.", getRemoteResourceCrn());
        } catch (Exception e) {
            LOGGER.error(String.format("Failed to execute duplicated secret migration for stack %s", getRemoteResourceCrn()), e);
        }
    }
}
