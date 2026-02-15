package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith({MockitoExtension.class})
public class BackUpDecoratorTest {

    private static final String RESOURCE = "440ac57e-9f21-4b9a-bcfd-3034a5738b12";

    private static final String BUCKET_NAME = "test-bucket";

    private static final String RESOURCE_CRN = String.format("crn:cdp:cloudbreak:us-west-1:default:stack:%s", RESOURCE);

    private static final String STACK_NAME = "stack";

    private static final String REGION = "region";

    @Mock
    private LoggingResponse loggingResponse;

    @Mock
    private DetailedEnvironmentResponse environmentResponse;

    @Mock
    private StackDto stackDto;

    @Mock
    private StackView stack;

    private BackUpDecorator backUpDecorator;

    private Map<String, SaltPillarProperties> servicePillar;

    @BeforeEach
    void setUp() {
        backUpDecorator = new BackUpDecorator();
        servicePillar = new HashMap<>();
        TelemetryResponse telemetryResponse = mock(TelemetryResponse.class);
        when(telemetryResponse.getLogging()).thenReturn(loggingResponse);
        when(environmentResponse.getTelemetry()).thenReturn(telemetryResponse);
        when(stackDto.getResourceCrn()).thenReturn(RESOURCE_CRN);
        when(stackDto.getRegion()).thenReturn(REGION);
        when(stack.getType()).thenReturn(StackType.DATALAKE);
        when(stackDto.getName()).thenReturn(STACK_NAME);
        when(stackDto.getStack()).thenReturn(stack);
    }

    private static Object[][] testData() {
        return new Object[][]{
                {"testLocation"},
                {""},
        };
    }

    @ParameterizedTest
    @MethodSource("testData")
    void decoratePillarWithBackupUsingBackUpLocation(String folder) {
        when(environmentResponse.getBackupLocation()).thenReturn(getBackupLocation(folder));
        backUpDecorator.decoratePillarWithBackup(stackDto, environmentResponse, servicePillar);
        verifyResult(folder);
    }

    @ParameterizedTest
    @MethodSource("testData")
    void decoratePillarWithBackupUsingTelemetry(String folder) {
        when(loggingResponse.getStorageLocation()).thenReturn(getBackupLocation(folder));
        backUpDecorator.decoratePillarWithBackup(stackDto, environmentResponse, servicePillar);
        verifyResult(folder);
    }

    @Test
    void decoratePillarWithBackupLocationNotSpecified() {
        assertThatThrownBy(() -> backUpDecorator.decoratePillarWithBackup(stackDto, environmentResponse, servicePillar))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Backup Location is empty");
    }

    private void verifyResult(String folder) {
        SaltPillarProperties cdpluksvolumebackupSaltPillar = servicePillar.get("cdpluksvolumebackup");
        assertNotNull(cdpluksvolumebackupSaltPillar);
        assertEquals("/cdpluksvolumebackup/init.sls", cdpluksvolumebackupSaltPillar.getPath());
        Map<String, String> cdpluksvolumebackupProperties = (Map<String, String>) cdpluksvolumebackupSaltPillar.getProperties().get("cdpluksvolumebackup");
        assertEquals(getExpectedBackupLocation(folder), cdpluksvolumebackupProperties.get("backup_location"));
        assertEquals(REGION, cdpluksvolumebackupProperties.get("aws_region"));
    }

    private String getExpectedBackupLocation(String folder) {
        return String.format("s3://%s/cluster-backups/datalake/%s_%s",
                StringUtils.isBlank(folder) ? BUCKET_NAME : String.format("%s/%s", BUCKET_NAME, folder), STACK_NAME, RESOURCE);
    }

    private String getBackupLocation(String folder) {
        return String.format("s3a://%s", StringUtils.isBlank(folder) ? BUCKET_NAME : String.format("%s/%s", BUCKET_NAME, folder));
    }
}
