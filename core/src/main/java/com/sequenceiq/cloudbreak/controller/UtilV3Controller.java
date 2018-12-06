package com.sequenceiq.cloudbreak.controller;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.UtilV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.VersionCheckResult;
import com.sequenceiq.cloudbreak.api.model.datalake.DatalakePrerequisiteRequest;
import com.sequenceiq.cloudbreak.api.model.datalake.DatalakePrerequisiteResponse;
import com.sequenceiq.cloudbreak.api.model.filesystem.CloudStorageSupportedResponse;
import com.sequenceiq.cloudbreak.api.model.rds.RDSBuildRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RdsBuildResult;
import com.sequenceiq.cloudbreak.api.model.stack.StackMatrix;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionBuilder;
import com.sequenceiq.cloudbreak.service.StackMatrixService;
import com.sequenceiq.cloudbreak.service.datalake.DatalakePrerequisiteService;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemSupportMatrixService;
import com.sequenceiq.cloudbreak.util.ClientVersionUtil;

@Controller
public class UtilV3Controller extends NotificationController implements UtilV3Endpoint {

    @Inject
    private RdsConnectionBuilder rdsConnectionBuilder;

    @Inject
    private StackMatrixService stackMatrixService;

    @Inject
    private FileSystemSupportMatrixService fileSystemSupportMatrixService;

    @Inject
    private DatalakePrerequisiteService datalakePrerequisiteService;

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
    public RdsBuildResult buildRdsConnection(@Valid RDSBuildRequest rdsBuildRequest, Set<String> targets) {
        RdsBuildResult rdsBuildResult = new RdsBuildResult();
        try {
            String clusterName = rdsBuildRequest.getClusterName().replaceAll("[^a-zA-Z0-9]", "");
            Map<String, String> result = rdsConnectionBuilder.buildRdsConnection(
                    rdsBuildRequest.getRdsConfigRequest().getConnectionURL(),
                    rdsBuildRequest.getRdsConfigRequest().getConnectionUserName(),
                    rdsBuildRequest.getRdsConfigRequest().getConnectionPassword(),
                    clusterName,
                    targets);

            rdsBuildResult.setResults(result);
        } catch (BadRequestException e) {
            throw new BadRequestException("Could not create databases in metastore - " + e.getMessage(), e);
        }
        return rdsBuildResult;
    }

    @Override
    public StackMatrix getStackMatrix() {
        return stackMatrixService.getStackMatrix();
    }

    @Override
    public Collection<CloudStorageSupportedResponse> getCloudStorageMatrix(String stackVersion) {
        return fileSystemSupportMatrixService.getCloudStorageMatrix(stackVersion);
    }

    @Override
    public DatalakePrerequisiteResponse buildDatalakePrerequisite(Long workspaceId, String environment,
            DatalakePrerequisiteRequest datalakePrerequisiteRequest) {
        return datalakePrerequisiteService.prepare(workspaceId, environment, datalakePrerequisiteRequest);
    }
}
