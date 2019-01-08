package com.sequenceiq.cloudbreak.controller;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.UtilV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4BuildResponse;
import com.sequenceiq.cloudbreak.api.model.VersionCheckResult;
import com.sequenceiq.cloudbreak.api.model.filesystem.CloudStorageSupportedResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4BuildRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackMatrix;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionBuilder;
import com.sequenceiq.cloudbreak.service.StackMatrixService;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemSupportMatrixService;
import com.sequenceiq.cloudbreak.util.ClientVersionUtil;

@Controller
public class UtilV3Controller implements UtilV3Endpoint {

    @Inject
    private RdsConnectionBuilder rdsConnectionBuilder;

    @Inject
    private StackMatrixService stackMatrixService;

    @Inject
    private FileSystemSupportMatrixService fileSystemSupportMatrixService;

    @Value("${info.app.version:}")
    private String cbVersion;

    @Override
    public VersionCheckResult checkClientVersion(String version) {
        boolean compatible = ClientVersionUtil.checkVersion(cbVersion, version);
        if (compatible) {
            return new VersionCheckResult(true);

        }
        return new VersionCheckResult(false, String.format("Versions not compatible: [server: '%s', client: '%s']", cbVersion, version));
    }

    @Override
    public DatabaseV4BuildResponse buildRdsConnection(@Valid DatabaseV4BuildRequest databaseV4BuildRequest, Set<String> targets) {
        DatabaseV4BuildResponse databaseV4BuildResponse = new DatabaseV4BuildResponse();
        try {
            String clusterName = databaseV4BuildRequest.getClusterName().replaceAll("[^a-zA-Z0-9]", "");
            Map<String, String> result = rdsConnectionBuilder.buildRdsConnection(
                    databaseV4BuildRequest.getRdsConfigRequest().getConnectionURL(),
                    databaseV4BuildRequest.getRdsConfigRequest().getConnectionUserName(),
                    databaseV4BuildRequest.getRdsConfigRequest().getConnectionPassword(),
                    clusterName,
                    targets);

            databaseV4BuildResponse.setResults(result);
        } catch (BadRequestException e) {
            throw new BadRequestException("Could not create databases in metastore - " + e.getMessage(), e);
        }
        return databaseV4BuildResponse;
    }

    @Override
    public StackMatrix getStackMatrix() {
        return stackMatrixService.getStackMatrix();
    }

    @Override
    public Collection<CloudStorageSupportedResponse> getCloudStorageMatrix(String stackVersion) {
        return fileSystemSupportMatrixService.getCloudStorageMatrix(stackVersion);
    }
}
