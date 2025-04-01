package com.sequenceiq.mock.clouderamanager.custom;

import jakarta.inject.Inject;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.sequenceiq.cloudbreak.sdx.RdcConstants;
import com.sequenceiq.mock.clouderamanager.ResponseCreatorComponent;
import com.sequenceiq.mock.swagger.model.ApiEndPoint;
import com.sequenceiq.mock.swagger.model.ApiMapEntry;
import com.sequenceiq.mock.swagger.model.ApiRemoteDataContext;

@Controller
@RequestMapping(value = "/{mockUuid}/api")
public class CustomCmController {

    @Inject
    private ResponseCreatorComponent responseCreatorComponent;

    @RequestMapping(value = "/cdp/remoteContext/byCluster/{clusterName}",
            produces = { "application/json" },
            method = RequestMethod.GET)
    public ResponseEntity<ApiRemoteDataContext> getRemoteContextByCluster(@PathVariable("mockUuid") String mockUuid,
            @PathVariable("clusterName") String clusterName) {
        ApiRemoteDataContext apiRemoteDataContext = new ApiRemoteDataContext();
        ApiEndPoint hmsApiEndpoint = new ApiEndPoint();
        hmsApiEndpoint.setName("hive");
        hmsApiEndpoint.setServiceType(RdcConstants.HIVE_SERVICE);
        hmsApiEndpoint.addServiceConfigsItem(apiMapEntry(RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_HOST, "mock_database"));
        hmsApiEndpoint.addServiceConfigsItem(apiMapEntry(RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_PORT, "5432"));
        hmsApiEndpoint.addServiceConfigsItem(apiMapEntry(RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_NAME, "hive"));
        hmsApiEndpoint.addServiceConfigsItem(apiMapEntry(RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_USER, "hive"));
        hmsApiEndpoint.addServiceConfigsItem(apiMapEntry(RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_PASSWORD, "pass"));
        apiRemoteDataContext.addEndPointsItem(hmsApiEndpoint);
        return responseCreatorComponent.exec(apiRemoteDataContext);
    }

    @RequestMapping(value = "/cdp/remoteContext",
            produces = { "application/json" },
            consumes = { "application/json" },
            method = RequestMethod.POST)
    public ResponseEntity<ApiRemoteDataContext> postRemoteContext(@PathVariable("mockUuid") String mockUuid, @Valid @RequestBody ApiRemoteDataContext body) {
        return responseCreatorComponent.exec(body);
    }

    private ApiMapEntry apiMapEntry(String key, String value) {
        ApiMapEntry apiMapEntry = new ApiMapEntry();
        apiMapEntry.setKey(key);
        apiMapEntry.setValue(value);
        return apiMapEntry;
    }
}
