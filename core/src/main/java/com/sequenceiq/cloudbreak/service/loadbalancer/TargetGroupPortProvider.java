package com.sequenceiq.cloudbreak.service.loadbalancer;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.oozie.OozieHAConfigProvider.OOZIE_HTTPS_PORT;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.HealthProbeParameters;
import com.sequenceiq.cloudbreak.cloud.model.NetworkProtocol;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;

@Service
public class TargetGroupPortProvider {

    @Value("${cb.https.port:443}")
    private String httpsPort;

    @Value("${cb.knox.port:8443}")
    private String knoxServicePort;

    @Value("${cb.loadBalancer.health-check.path}")
    private String healthCheckPath;

    @Value("${cb.loadBalancer.health-check.port}")
    private int healthCheckPort;

    @Value("${cb.loadBalancer.health-check.protocol}")
    private String healthCheckProtocol;

    @Value("${cb.loadBalancer.health-check.interval}")
    private int healthCheckInterval;

    @Value("${cb.loadBalancer.health-check.probeDownThreshold}")
    private int healthCheckProbeDownThreshold;

    public Set<TargetGroupPortPair> getTargetGroupPortPairs(TargetGroup targetGroup) {
        switch (targetGroup.getType()) {
            case KNOX:
                return Set.of(new TargetGroupPortPair(Integer.parseInt(httpsPort),
                        NetworkProtocol.TCP,
                        new HealthProbeParameters(healthCheckPath, Integer.parseInt(knoxServicePort), NetworkProtocol.valueOf(healthCheckProtocol),
                                healthCheckInterval, healthCheckProbeDownThreshold)));
            case OOZIE:
                return Set.of(new TargetGroupPortPair(Integer.parseInt(OOZIE_HTTPS_PORT),
                        NetworkProtocol.TCP,
                        new HealthProbeParameters(healthCheckPath, Integer.parseInt(OOZIE_HTTPS_PORT), NetworkProtocol.valueOf(healthCheckProtocol),
                                healthCheckInterval, healthCheckProbeDownThreshold)));
            case OOZIE_GCP:
                return Set.of(new TargetGroupPortPair(Integer.parseInt(OOZIE_HTTPS_PORT),
                        NetworkProtocol.TCP,
                        new HealthProbeParameters(healthCheckPath, Integer.parseInt(knoxServicePort), NetworkProtocol.valueOf(healthCheckProtocol),
                                healthCheckInterval, healthCheckProbeDownThreshold)));
            default:
                return Set.of();
        }
    }
}
