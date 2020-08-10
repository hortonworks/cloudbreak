package com.sequenceiq.periscope.client;

import javax.ws.rs.client.WebTarget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClient;
import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceEndpoint;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.periscope.api.AutoscaleApi;
import com.sequenceiq.periscope.api.endpoint.v1.AlertEndpoint;
import com.sequenceiq.periscope.api.endpoint.v1.AutoScaleClusterV1Endpoint;
import com.sequenceiq.periscope.api.endpoint.v1.ConfigurationEndpoint;
import com.sequenceiq.periscope.api.endpoint.v1.DistroXAutoScaleClusterV1Endpoint;
import com.sequenceiq.periscope.api.endpoint.v1.HistoryEndpoint;
import com.sequenceiq.periscope.api.endpoint.v1.PolicyEndpoint;

public class AutoscaleUserCrnClient extends AbstractUserCrnServiceClient<AutoscaleUserCrnClient.AutoscaleEndpoint> {

    private final Logger logger = LoggerFactory.getLogger(AutoscaleUserCrnClient.class);

    public AutoscaleUserCrnClient(String autoscaleAddress, ConfigKey configKey) {
        super(autoscaleAddress, configKey, AutoscaleApi.API_ROOT_CONTEXT);
        logger.info("AutoscaleClient has been created: {}, configKey: {}", autoscaleAddress, configKey);
    }

    public AutoscaleEndpoint withCrn(String crn) {
        return new AutoscaleEndpoint(getWebTarget(), crn);
    }

    public static class AutoscaleEndpoint extends AbstractUserCrnServiceEndpoint {

        public AutoscaleEndpoint(WebTarget webTarget, String crn) {
            super(webTarget, crn);
        }

        public AlertEndpoint alertEndpoint() {
            return getEndpoint(AlertEndpoint.class);
        }

        public DistroXAutoScaleClusterV1Endpoint distroXAutoScaleClusterV1Endpoint() {
            return getEndpoint(DistroXAutoScaleClusterV1Endpoint.class);
        }

        public AutoScaleClusterV1Endpoint clusterEndpoint() {
            return getEndpoint(AutoScaleClusterV1Endpoint.class);
        }

        public ConfigurationEndpoint configurationEndpoint() {
            return getEndpoint(ConfigurationEndpoint.class);
        }

        public HistoryEndpoint historyEndpoint() {
            return getEndpoint(HistoryEndpoint.class);
        }

        public PolicyEndpoint policyEndpoint() {
            return getEndpoint(PolicyEndpoint.class);
        }
    }
}
