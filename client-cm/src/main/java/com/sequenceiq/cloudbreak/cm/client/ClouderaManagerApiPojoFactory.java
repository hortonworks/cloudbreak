package com.sequenceiq.cloudbreak.cm.client;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.ParcelResourceApi;
import com.cloudera.api.swagger.ParcelsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.cm.client.retry.CmApiRetryAspect;

/**
 * The CM API objects this factory provides are not Spring managed bean.
 * Use this API if you don't want retry mechanism provided by the {@link CmApiRetryAspect}.
 * Particularly useful in poller tasks, where the retry mechanism is built into the poller tasks
 * and the aspect based would be unnecessary redundancy.
 */
@Component
public class ClouderaManagerApiPojoFactory {

    public CommandsResourceApi getCommandsResourceApi(ApiClient apiClient) {
        return new CommandsResourceApi(apiClient);
    }

    public ParcelsResourceApi getParcelsResourceApi(ApiClient apiClient) {
        return new ParcelsResourceApi(apiClient);
    }

    public ParcelResourceApi getParcelResourceApi(ApiClient apiClient) {
        return new ParcelResourceApi(apiClient);
    }

    public HostsResourceApi getHostsResourceApi(ApiClient apiClient) {
        return new HostsResourceApi(apiClient);
    }
}
