package com.sequenceiq.cloudbreak.cm;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;

@Service
public class ClouderaManagerSupportSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerSupportSetupService.class);

    private static final String CREATOR_TAG = "CREATOR_TAG";

    @Value("${info.app.version:}")
    private String cbVersion;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    void prepareSupportRole(ApiClient apiClient, StackType type) {
        try {
            clouderaManagerApiFactory.getClouderaManagerResourceApi(apiClient).updateConfig("",
                new ApiConfigList().addItemsItem(
                    new ApiConfig()
                        .name(CREATOR_TAG)
                        .value(String.format("Cloudera %s %s", getServiceType(type), cbVersion))
                ));
        } catch (ApiException e) {
            LOGGER.debug("Failed to set CREATOR_TAG on Cloudera Manager", e);
            throw new ClouderaManagerOperationFailedException("Failed to set CREATOR_TAG on Cloudera Manager", e);
        }
    }

    private String getServiceType(StackType stackType) {
        return stackType == StackType.WORKLOAD ? "Data Hub" : "Data Lake";
    }

}
