package com.sequenceiq.periscope.monitor.evaluator;

import static com.sequenceiq.periscope.monitor.evaluator.ScalingConstants.DEFAULT_CLUSTER_COOLDOWN_MINS;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.util.JaxRSUtil;
import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.PrometheusAlert;
import com.sequenceiq.periscope.model.PrometheusResponse;
import com.sequenceiq.periscope.model.PrometheusResponse.Result;
import com.sequenceiq.periscope.model.TlsConfiguration;
import com.sequenceiq.periscope.monitor.context.ClusterIdEvaluatorContext;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.repository.PrometheusAlertRepository;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.security.TlsSecurityService;

@Component("PrometheusEvaluator")
@Scope("prototype")
public class PrometheusEvaluator extends EvaluatorExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusEvaluator.class);

    private static final String EVALUATOR_NAME = PrometheusEvaluator.class.getName();

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private PrometheusAlertRepository alertRepository;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private EventPublisher eventPublisher;

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
        long start = System.currentTimeMillis();
        try {
            Cluster cluster = clusterService.findById(clusterId);
            if (!isCoolDownTimeElapsed(cluster.getStackCrn(), DEFAULT_CLUSTER_COOLDOWN_MINS, cluster.getLastScalingActivity())) {
                return;
            }

            MDCBuilder.buildMdcContext(cluster);
            TlsConfiguration tlsConfig = tlsSecurityService.getTls(clusterId);
            Client client = RestClientUtil.createClient(tlsConfig.getServerCert(),
                    tlsConfig.getClientCert(), tlsConfig.getClientKey(), true);
            String prometheusAddress = String.format("https://%s:%s/prometheus", cluster.getClusterManager().getHost(), cluster.getPort());
            WebTarget target = client.target(prometheusAddress);

            for (PrometheusAlert alert : alertRepository.findAllByCluster(clusterId)) {
                String alertName = alert.getName();
                LOGGER.debug("Checking Prometheus based alert: '{}'", alertName);
                String query = URLEncoder.encode(String.format("ALERTS{alertname=\"%s\"}[%dm]", alert.getName(), alert.getPeriod()), StandardCharsets.UTF_8);
                Response response = target
                        .path("/api/v1/query")
                        .queryParam("query", query)
                        .request()
                        .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                        .get();

                PrometheusResponse prometheusResponse = JaxRSUtil.response(response, PrometheusResponse.class);

                boolean triggerScale = false;
                switch (alert.getAlertState()) {
                    case OK:
                        triggerScale = prometheusResponse.getData().getResult().isEmpty();
                        break;

                    case CRITICAL:
                        for (Result alertResult : prometheusResponse.getData().getResult()) {
                            if ("firing".equals(alertResult.getMetric().getAlertstate())) {
                                List<Object> lastSample = alertResult.getValues().get(alertResult.getValues().size() - 1);
                                Object alertValue = lastSample.get(1);
                                if (alertValue instanceof String) {
                                    if ("0".equals(alertValue)) {
                                        break;
                                    }
                                    triggerScale = true;
                                }
                            }
                        }
                        break;

                    default:
                        triggerScale = false;
                        break;
                }

                if (triggerScale && isPolicyAttached(alert)) {
                    eventPublisher.publishEvent(new ScalingEvent(alert));
                }
            }
        } catch (Exception e) {
            LOGGER.info("Failed to retrieve alerts from Prometheus", e);
            eventPublisher.publishEvent(new UpdateFailedEvent(clusterId));
        } finally {
            LOGGER.debug("Finished prometheusEvaluator for cluster {} in {} ms", clusterId, System.currentTimeMillis() - start);
        }
    }

    private boolean isPolicyAttached(BaseAlert alert) {
        return alert.getScalingPolicy() != null;
    }

}
