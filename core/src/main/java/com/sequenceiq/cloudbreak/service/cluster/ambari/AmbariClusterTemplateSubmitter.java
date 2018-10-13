package com.sequenceiq.cloudbreak.service.cluster.ambari;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.io.CharStreams;
import com.sequenceiq.ambari.client.services.ClusterService;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.util.JsonUtil;

import groovyx.net.http.HttpResponseException;

@Service
public class AmbariClusterTemplateSubmitter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterTemplateSubmitter.class);

    private static final String KEY_TYPE = "PERSISTED";

    @Inject
    private AmbariClusterTemplateGenerator ambariClusterTemplateGenerator;

    void addClusterTemplate(Cluster cluster, Map<String, List<Map<String, String>>> hostGroupMappings, ClusterService ambariClient) throws CloudbreakException {
        String clusterName = cluster.getName();

        if (ambariClient.getClusterName() == null) {
            try {
                String clusterTemplate = ambariClusterTemplateGenerator.generateClusterTemplate(cluster, hostGroupMappings, ambariClient);
                LOGGER.info("Submitted cluster creation template: {}", JsonUtil.minify(clusterTemplate, Collections.singleton("credentials")));
                ambariClient.createClusterFromTemplate(clusterName, clusterTemplate);
            } catch (HttpResponseException exception) {
                String reason = collectErrorReason(exception);
                String msg = String.format("Ambari client failed to apply cluster creation template! Reason: %s", reason);
                LOGGER.error(msg, exception);
                throw new AmbariServiceException(msg, exception);
            }
        } else {
            LOGGER.info("Ambari cluster already exists: {}", clusterName);
        }
    }

    private String collectErrorReason(HttpResponseException exception) {
        Object data = exception.getResponse().getData();
        String reason;
        try {
            if (data instanceof Readable) {
                reason = CharStreams.toString((Readable) data);
            } else if (data != null) {
                reason = data.toString();
            } else {
                reason = "No response from Ambari Server!";
            }
        } catch (IOException e) {
            reason = "Ambari server response cannot be parsed!";
        }
        return reason;
    }

}
