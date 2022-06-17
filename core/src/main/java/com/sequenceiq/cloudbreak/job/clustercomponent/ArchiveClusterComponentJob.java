package com.sequenceiq.cloudbreak.job.clustercomponent;

import java.util.Collections;
import java.util.Optional;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import io.opentracing.Tracer;

@Component
@DisallowConcurrentExecution
public class ArchiveClusterComponentJob extends StatusCheckerJob {

    private final ClusterComponentConfigProvider clusterComponentConfigProvider;

    private final StackService stackService;

    private TransactionService transactionService;

    public ArchiveClusterComponentJob(Tracer tracer, ClusterComponentConfigProvider clusterComponentConfigProvider, StackService stackService) {
        super(tracer, "Archive ClusterComponent Job");
        this.clusterComponentConfigProvider = clusterComponentConfigProvider;
        this.stackService = stackService;
    }

    @Override
    protected Object getMdcContextObject() {
        return Optional.ofNullable(clusterComponentConfigProvider.getComponentsByClusterId(getClusterId())).orElseGet(Collections::emptySet);
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        try {

        } catch (Exception e) {

        }
    }

    private Long getClusterId() {
        return stackService.get(getStackId()).getCluster().getId();
    }

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }

}
