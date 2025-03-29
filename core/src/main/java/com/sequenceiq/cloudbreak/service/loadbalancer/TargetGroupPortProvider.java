package com.sequenceiq.cloudbreak.service.loadbalancer;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.oozie.OozieHAConfigProvider.OOZIE_HTTPS_PORT;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;

@Service
public class TargetGroupPortProvider {

    @Value("${cb.https.port:443}")
    private String httpsPort;

    @Value("${cb.knox.port:8443}")
    private String knoxServicePort;

    public Set<TargetGroupPortPair> getTargetGroupPortPairs(TargetGroup targetGroup) {
        switch (targetGroup.getType()) {
            case KNOX:
                return Set.of(new TargetGroupPortPair(Integer.parseInt(httpsPort), Integer.parseInt(knoxServicePort)));
            case OOZIE:
                return Set.of(new TargetGroupPortPair(Integer.parseInt(OOZIE_HTTPS_PORT), Integer.parseInt(OOZIE_HTTPS_PORT)));
            case OOZIE_GCP:
                return Set.of(new TargetGroupPortPair(Integer.parseInt(OOZIE_HTTPS_PORT), Integer.parseInt(knoxServicePort)));
            default:
                return Set.of();
        }
    }
}
