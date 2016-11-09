package com.sequenceiq.periscope.monitor.evaluator;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.MetricAlert;
import com.sequenceiq.periscope.log.MDCBuilder;
import com.sequenceiq.periscope.model.PrometheusResponse;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.repository.MetricAlertRepository;
import com.sequenceiq.periscope.service.ClusterService;

@Component("PrometheusEvaluator")
@Scope("prototype")
public class PrometheusEvaluator extends AbstractEventPublisher implements EvaluatorExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusEvaluator.class);

    @Value("${prometheus.address}")
    private String prometheusAddress;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private MetricAlertRepository alertRepository;

    private long clusterId;

    @Override
    public void setContext(Map<String, Object> context) {
        this.clusterId = (long) context.get(EvaluatorContext.CLUSTER_ID.name());
    }

    @Override
    public void run() {
        Cluster cluster = clusterService.find(clusterId);
        MDCBuilder.buildMdcContext(cluster);
        RestTemplate restTemplate = new RestTemplate();
        try {
            for (MetricAlert alert : alertRepository.findAllByCluster(clusterId)) {
                String alertName = alert.getName();
                LOGGER.info("Checking metric based alert: '{}'", alertName);
                UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(prometheusAddress + "/api/v1/query")
                        .queryParam("query", String.format("ALERTS{alertname=\"%s\"}[%dm]", alert.getDefinitionName(), alert.getPeriod())).build();
                HttpHeaders headers = new HttpHeaders();
                headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
                HttpEntity<?> entity = new HttpEntity<>(headers);
                HttpEntity<PrometheusResponse> resp = restTemplate.exchange(uriComponents.encode().toUri(), HttpMethod.GET, entity, PrometheusResponse.class);
                PrometheusResponse prometheusResponse = resp.getBody();

                boolean triggerScale = false;
                switch (alert.getAlertState()) {
                    case OK:
                        triggerScale = prometheusResponse.getData().getResult().isEmpty();
                        break;

                    case CRITICAL:
                        for (PrometheusResponse.Result alertResult : prometheusResponse.getData().getResult()) {
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
                    publishEvent(new ScalingEvent(alert));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve alert history", e);
            publishEvent(new UpdateFailedEvent(clusterId));
        }
    }

    private boolean isPolicyAttached(BaseAlert alert) {
        return alert.getScalingPolicy() != null;
    }

}
