package com.sequenceiq.periscope.client;

import static com.sequenceiq.cloudbreak.auth.altus.CrnTokenExtractor.CRN_HEADER;

import java.util.Collections;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.periscope.api.AutoscaleApi;
import com.sequenceiq.periscope.api.endpoint.v1.AlertEndpoint;
import com.sequenceiq.periscope.api.endpoint.v1.AutoScaleClusterV1Endpoint;
import com.sequenceiq.periscope.api.endpoint.v1.ConfigurationEndpoint;
import com.sequenceiq.periscope.api.endpoint.v1.HistoryEndpoint;
import com.sequenceiq.periscope.api.endpoint.v1.PolicyEndpoint;

public class AutoscaleUserCrnClient {

    private static final Form EMPTY_FORM = new Form();

    private final Logger logger = LoggerFactory.getLogger(AutoscaleUserCrnClient.class);

    private final Client client;

    private WebTarget webTarget;

    public AutoscaleUserCrnClient(String autoscaleAddress, ConfigKey configKey) {
        client = RestClientUtil.get(configKey);
        webTarget = client.target(autoscaleAddress).path(AutoscaleApi.API_ROOT_CONTEXT);
        logger.info("AutoscaleClient has been created: {}, configKey: {}", autoscaleAddress, configKey);
    }

    public AutoscaleEndpoint withCrn(String crn) {
        return new AutoscaleEndpoint(webTarget, crn);
    }

    public static class AutoscaleEndpoint {

        private WebTarget webTarget;

        private String crn;

        public AutoscaleEndpoint(WebTarget webTarget, String crn) {
            this.webTarget = webTarget;
            this.crn = crn;
        }

        public AlertEndpoint alertEndpoint() {
            return getEndpoint(AlertEndpoint.class);
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

        protected <E> E getEndpoint(Class<E> clazz) {
            MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(CRN_HEADER, crn);
            return newEndpoint(clazz, headers);
        }

        private <C> C newEndpoint(Class<C> resourceInterface, MultivaluedMap<String, Object> headers) {
            return WebResourceFactory.newResource(resourceInterface, webTarget, false, headers, Collections.emptyList(), EMPTY_FORM);
        }
    }
}
