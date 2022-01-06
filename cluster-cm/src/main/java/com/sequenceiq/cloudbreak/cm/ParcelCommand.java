package com.sequenceiq.cloudbreak.cm;

import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;

@FunctionalInterface
public interface ParcelCommand {
    ApiCommand apply(String clusterName, String product, String version) throws ApiException;
}
