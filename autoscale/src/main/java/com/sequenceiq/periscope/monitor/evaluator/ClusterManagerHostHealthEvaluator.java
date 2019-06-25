package com.sequenceiq.periscope.monitor.evaluator;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.FailureReportV4Request;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterManagerVariant;
import com.sequenceiq.periscope.monitor.context.ClusterIdEvaluatorContext;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.configuration.CloudbreakClientConfiguration;

@Component("ClusterManagerHostHealthEvaluator")
@Scope("prototype")
public class ClusterManagerHostHealthEvaluator extends EvaluatorExecutor {

    private static final String EVALUATOR_NAME = ClusterManagerHostHealthEvaluator.class.getName();

    @Inject
    private ClusterService clusterService;

    @Inject
    private Map<ClusterManagerVariant, ClusterManagerSpecificHostHealthEvaluator> hostHealthEvaluatorMap;

    @Inject
    private CloudbreakClientConfiguration cloudbreakClientConfiguration;

    private long clusterId;

    @Override
    public void setContext(EvaluatorContext context) {
        clusterId = (long) context.getData();
    }

    @Override
    @Nonnull
    public EvaluatorContext getContext() {
        return new ClusterIdEvaluatorContext(clusterId);
    }

    @Override
    public String getName() {
        return EVALUATOR_NAME;
    }

    @Override
    public void execute() {
        Cluster cluster = clusterService.findById(clusterId);
        ClusterManagerVariant variant = cluster.getClusterManager().getVariant();
        ClusterManagerSpecificHostHealthEvaluator clusterManagerSpecificHostHealthEvaluator = hostHealthEvaluatorMap.get(variant);
        List<String> hostNamesToRecover = clusterManagerSpecificHostHealthEvaluator.determineHostnamesToRecover(cluster);
        if (!CollectionUtils.isEmpty(hostNamesToRecover)) {
            CloudbreakInternalCrnClient cbClient = cloudbreakClientConfiguration.cloudbreakInternalCrnClientClient();
            FailureReportV4Request failureReport = new FailureReportV4Request();
            failureReport.setFailedNodes(hostNamesToRecover);
            cbClient.withInternalCrn().autoscaleEndpoint().failureReport(cluster.getStackCrn(), failureReport);
        }
    }
}
