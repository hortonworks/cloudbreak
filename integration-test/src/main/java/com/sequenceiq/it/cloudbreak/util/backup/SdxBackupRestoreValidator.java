package com.sequenceiq.it.cloudbreak.util.backup;

import static java.lang.String.format;

import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.model.SdxBackupStatusResponse;
import com.sequenceiq.sdx.api.model.SdxRestoreStatusResponse;

@Component
@Scope("prototype")
public class SdxBackupRestoreValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxBackupRestoreValidator.class);

    private String backupId;

    private String restoreId;

    public String getBackupId() {
        return backupId;
    }

    public String getRestoreId() {
        return restoreId;
    }

    public SdxInternalTestDto validateDatalakeStatus(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) {
        String statusReason = client.getDefaultClient(testContext)
                .sdxEndpoint()
                .getDetailByCrn(testDto.getCrn(), Collections.emptySet())
                .getStatusReason();
        if (statusReason.contains("Data Lake backup failed")) {
            LOGGER.error(" Data Lake '{}' backup has been failed: '{}' ", testDto.getName(), statusReason);
            throw new TestFailException(String.format(" Data Lake '%s' backup has been failed: '%s' ", testDto.getName(), statusReason));
        } else if (statusReason.contains("Data Lake restore failed")) {
            LOGGER.error(" Data Lake '{}' restore has been failed: '{}' ", testDto.getName(), statusReason);
            throw new TestFailException(String.format(" Data Lake '%s' restore has been failed: '%s' ", testDto.getName(), statusReason));
        } else {
            LOGGER.info(" Data Lake '{}' backup/restore has been done with '{}'. ", testDto.getName(), statusReason);
            Log.then(LOGGER, format(" Data Lake '%s' backup/restore has been done with '%s'. ", testDto.getName(), statusReason));
        }
        return testDto;
    }

    public SdxInternalTestDto validateDatalakeBackupStatus(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) {
        String sdxName = testDto.getName();
        backupId = client.getDefaultClient(testContext)
                .sdxBackupEndpoint()
                .getDatalakeBackupId(sdxName, null);
        SdxBackupStatusResponse sdxBackupStatusResponse = client.getDefaultClient(testContext)
                .sdxBackupEndpoint()
                .getBackupDatalakeStatus(sdxName, backupId, null);
        String status = sdxBackupStatusResponse.getStatus();
        String statusReason = sdxBackupStatusResponse.getReason();
        LOGGER.info(" Data Lake '{}' backup '{}' status '{}', because of {} ", sdxName, backupId, status, statusReason);
        if (status.contains("FAILED")) {
            LOGGER.error(" Data Lake '{}' backup has been failed: '{}' ", testDto.getName(), statusReason);
            throw new TestFailException(String.format(" Data Lake '%s' backup has been failed: '%s' ", testDto.getName(), statusReason));
        } else {
            LOGGER.info(" Data Lake '{}' backup has been done with '{}'. ", testDto.getName(), statusReason);
            Log.then(LOGGER, format(" Data Lake '%s' backup has been done with '%s'. ", testDto.getName(), statusReason));
        }
        return testDto;
    }

    public SdxInternalTestDto validateDatalakeRestoreStatus(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) {
        String sdxName = testDto.getName();
        String status;
        String statusReason;

        try {
            restoreId = client.getDefaultClient(testContext)
                    .sdxRestoreEndpoint()
                    .getDatalakeRestoreId(sdxName, null);
            SdxRestoreStatusResponse sdxRestoreStatusResponse = client.getDefaultClient(testContext)
                    .sdxRestoreEndpoint()
                    .getRestoreDatalakeStatus(sdxName, restoreId, null);
            status = sdxRestoreStatusResponse.getStatus();
            statusReason = sdxRestoreStatusResponse.getReason();
            LOGGER.info(" Data Lake '{}' restore '{}' status '{}', because of {} ", sdxName, restoreId, status, statusReason);
        } catch (Exception e) {
            if (e.getMessage().contains("not found")) {
                throw new TestFailException(String.format(" NOT FOUND :: Cannot get status information for restore '%s' on Data Lake '%s'." +
                                " Please check the selected backup was successful and the related backup ID is correct. ",
                        restoreId, testDto.getName()), e.getCause());
            }
            throw e;
        }

        if (StringUtils.isBlank(status)) {
            LOGGER.error(" Data Lake '{}' restore status is not available ", testDto.getName());
            throw new TestFailException(String.format(" Data Lake '%s' restore status is not available  ", testDto.getName()));
        } else if (status.contains("FAILED")) {
            LOGGER.error(" Data Lake '{}' restore has been failed: '{}' ", testDto.getName(), statusReason);
            throw new TestFailException(String.format(" Data Lake '%s' restore has been failed: '%s' ", testDto.getName(), statusReason));
        } else {
            LOGGER.info(" Data Lake '{}' restore has been done with '{}'. ", testDto.getName(), statusReason);
            Log.then(LOGGER, format(" Data Lake '%s' restore has been done with '%s'. ", testDto.getName(), statusReason));
        }
        return testDto;
    }
}
