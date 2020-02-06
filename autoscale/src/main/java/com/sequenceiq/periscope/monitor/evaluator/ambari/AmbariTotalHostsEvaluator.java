package com.sequenceiq.periscope.monitor.evaluator.ambari;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.periscope.aspects.RequestLogging;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterManagerVariant;
import com.sequenceiq.periscope.monitor.evaluator.ClusterManagerTotalHostsEvaluator;
import com.sequenceiq.periscope.service.AmbariClientProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component("AmbariTotalHostsEvaluator")
@Scope("prototype")
public class AmbariTotalHostsEvaluator implements ClusterManagerTotalHostsEvaluator {
    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private RequestLogging ambariRequestLogging;

    @Override
    public ClusterManagerVariant getSupportedClusterManagerVariant() {
        return ClusterManagerVariant.AMBARI;
    }

    @Override
    public int getTotalHosts(Cluster cluster) {
        AmbariClient ambariClient = ambariClientProvider.createAmbariClient(cluster);
        return ambariRequestLogging.logging(ambariClient::getClusterHosts, "clusterHosts").size();
    }
}
