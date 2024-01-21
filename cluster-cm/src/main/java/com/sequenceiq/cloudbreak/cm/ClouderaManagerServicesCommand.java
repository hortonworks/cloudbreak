package com.sequenceiq.cloudbreak.cm;

import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;

@FunctionalInterface
public interface ClouderaManagerServicesCommand {
    ApiCommand apply(String clusterName, String serviceName) throws ApiException;
}
