package com.sequenceiq.mock.clouderamanager.base.batchapi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.mock.clouderamanager.DataProviderService;
import com.sequenceiq.mock.swagger.model.ApiBatchRequestElement;
import com.sequenceiq.mock.swagger.model.ApiBatchResponseElement;
import com.sequenceiq.mock.swagger.model.ApiHost;
import com.sequenceiq.mock.swagger.model.HTTPMethod;

@Component
public class HostsResourceApiUpdateHostBatchApiHandler extends AbstractBatchApiHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(HostsResourceApiUpdateHostBatchApiHandler.class);

    private static final Pattern HOSTS_RESOURCE_API_UPDATE_HOST_PATTERN = Pattern.compile("^/api/v[0-9]+/hosts/(.+)$");

    private static final int GROUP_HOST_ID = 1;

    private final DataProviderService dataProviderService;

    public HostsResourceApiUpdateHostBatchApiHandler(DataProviderService dataProviderService) {
        this.dataProviderService = dataProviderService;
    }

    @Override
    public String getDescription() {
        return "Handler for the batch API request representing " +
                "com.cloudera.api.swagger.HostsResourceApi.updateHost(String hostId, com.cloudera.api.swagger.model.ApiHost).";
    }

    @Override
    public boolean canProcess(ApiBatchRequestElement apiBatchRequestElement) {
        Matcher matcher = HOSTS_RESOURCE_API_UPDATE_HOST_PATTERN.matcher(apiBatchRequestElement.getUrl());
        return matcher.matches() && apiBatchRequestElement.getMethod() == HTTPMethod.PUT;
    }

    @Override
    public ApiBatchResponseElement process(String mockUuid, ApiBatchRequestElement apiBatchRequestElement) {
        Matcher matcher = HOSTS_RESOURCE_API_UPDATE_HOST_PATTERN.matcher(apiBatchRequestElement.getUrl());
        if (matcher.matches()) {
            LOGGER.debug("Processing request. MockUuid: [{}]. Request: {}", mockUuid, apiBatchRequestElement);
            String hostId = matcher.group(GROUP_HOST_ID);
            Json jsonFromRequestBody = new Json(apiBatchRequestElement.getBody());
            ApiHost apiHostFromRequestBody = jsonFromRequestBody.getUnchecked(ApiHost.class);
            String rackId = apiHostFromRequestBody.getRackId();
            ApiHost apiHostFound = dataProviderService.getApiHost(mockUuid, hostId);
            apiHostFound.rackId(rackId);
            // Host updates could be persisted here, but now this is not necessary as ApiHost details are directly populated from SPI VM metadata.
            ApiBatchResponseElement apiBatchResponseElement = getSuccessApiBatchResponseElement(jsonFromRequestBody.getValue());
            LOGGER.debug("Processing request completed. MockUuid: [{}]. Response: {}", mockUuid, apiBatchResponseElement);
            return apiBatchResponseElement;
        } else {
            // Should not happen as this method ought to be only invoked if "canProcess()" previously returned true.
            throw new UnsupportedOperationException("This handler does not support processing the request " + apiBatchRequestElement);
        }
    }

}
