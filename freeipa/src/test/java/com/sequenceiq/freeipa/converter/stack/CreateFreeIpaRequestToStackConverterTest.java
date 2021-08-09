package com.sequenceiq.freeipa.converter.stack;

import static com.sequenceiq.freeipa.util.CloudArgsForIgConverter.DISK_ENCRYPTION_SET_ID;
import static com.sequenceiq.freeipa.util.CloudArgsForIgConverter.GCP_KMS_ENCRYPTION_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.api.model.Backup;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.StackAuthenticationRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.converter.authentication.StackAuthenticationRequestToStackAuthenticationConverter;
import com.sequenceiq.freeipa.converter.backup.BackupConverter;
import com.sequenceiq.freeipa.converter.instance.InstanceGroupRequestToInstanceGroupConverter;
import com.sequenceiq.freeipa.converter.telemetry.TelemetryConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.StackAuthentication;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.tag.AccountTagService;
import com.sequenceiq.freeipa.util.CrnService;
import com.sequenceiq.freeipa.util.CloudArgsForIgConverter;

@ExtendWith(MockitoExtension.class)
public class CreateFreeIpaRequestToStackConverterTest {

    private static final String ACCOUNT_ID = "accountId";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @InjectMocks
    private CreateFreeIpaRequestToStackConverter underTest;

    @Mock
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @Mock
    private StackAuthenticationRequestToStackAuthenticationConverter stackAuthenticationConverter;

    @Mock
    private InstanceGroupRequestToInstanceGroupConverter instanceGroupConverter;

    @Mock
    private TelemetryConverter telemetryConverter;

    @Mock
    private BackupConverter backupConverter;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private AccountTagService accountTagService;

    @Mock
    private CostTagging costTagging;

    @Mock
    private CrnService crnService;

    @Captor
    private ArgumentCaptor<EnumMap> mapCaptorForDiskEncryptionSetId;

    @Captor
    private ArgumentCaptor<EnumMap> mapCaptorForEncryptionKey;

    @Test
    void testConvertForInstanceGroupsWhendiskEncryptionSetIdIsPresent() throws InterruptedException, ExecutionException, TimeoutException {
        CreateFreeIpaRequest source = createCreateFreeIpaRequest();

        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setAzure(AzureEnvironmentParameters.builder()
                .withResourceEncryptionParameters(AzureResourceEncryptionParameters.builder()
                        .withDiskEncryptionSetId("dummyDiskEncryptionSetId")
                        .build())
                .build());
        when(crnService.createCrn(ACCOUNT_ID, CrnResourceDescriptor.FREEIPA)).thenReturn("resourceCrn");
        when(stackAuthenticationConverter.convert(source.getAuthentication())).thenReturn(new StackAuthentication());
        when(cachedEnvironmentClientService.getByCrn(source.getEnvironmentCrn())).thenReturn(environmentResponse);
        when(instanceGroupConverter.convert(any(InstanceGroupRequest.class), anyString(), anyString(),
                anyString(), anyString(), anyString(), any(EnumMap.class)))
                .thenReturn(new InstanceGroup());
        when(telemetryConverter.convert(source.getTelemetry())).thenReturn(new Telemetry());
        when(backupConverter.convert(source.getTelemetry())).thenReturn(new Backup());
        when(entitlementService.internalTenant(ACCOUNT_ID)).thenReturn(Boolean.FALSE);
        when(costTagging.prepareDefaultTags(any())).thenReturn(new HashMap<>());
        ReflectionTestUtils.setField(underTest, "userGetTimeout", 10L);
        ReflectionTestUtils.setField(underTest, "defaultGatewayCidr", Set.of());
        Future<String> owner = CompletableFuture.completedFuture("dummyUser");

        underTest.convert(source, ACCOUNT_ID, owner, "crn1", CloudPlatform.AZURE.name());
        verify(instanceGroupConverter).convert(any(InstanceGroupRequest.class), anyString(), anyString(), anyString(), anyString(), anyString(),
                mapCaptorForDiskEncryptionSetId.capture());
        assertEquals(mapCaptorForDiskEncryptionSetId.getValue().get(DISK_ENCRYPTION_SET_ID), "dummyDiskEncryptionSetId");
    }

    @Test
    void testConvertForInstanceGroupsWhenEncryptionKeyIsPresentForGcp() throws InterruptedException, ExecutionException, TimeoutException {
        CreateFreeIpaRequest source = createCreateFreeIpaRequest();

        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();

        environmentResponse.setGcp(GcpEnvironmentParameters.builder()
                .withResourceEncryptionParameters(GcpResourceEncryptionParameters.builder()
                        .withEncryptionKey("dummyEncryptionKey")
                        .build())
                .build());

        when(crnService.createCrn(ACCOUNT_ID, CrnResourceDescriptor.FREEIPA)).thenReturn("resourceCrn");
        when(stackAuthenticationConverter.convert(source.getAuthentication())).thenReturn(new StackAuthentication());
        when(cachedEnvironmentClientService.getByCrn(source.getEnvironmentCrn())).thenReturn(environmentResponse);
        when(instanceGroupConverter.convert(any(InstanceGroupRequest.class), anyString(), anyString(), anyString(),
                anyString(), anyString(), any(EnumMap.class))).thenReturn(new InstanceGroup());
        when(telemetryConverter.convert(source.getTelemetry())).thenReturn(new Telemetry());
        when(backupConverter.convert(source.getTelemetry())).thenReturn(new Backup());
        when(entitlementService.internalTenant(ACCOUNT_ID)).thenReturn(Boolean.FALSE);
        when(costTagging.prepareDefaultTags(any())).thenReturn(new HashMap<>());
        ReflectionTestUtils.setField(underTest, "userGetTimeout", 10L);
        ReflectionTestUtils.setField(underTest, "defaultGatewayCidr", Set.of());
        Future<String> owner = CompletableFuture.completedFuture("dummyUser");

        underTest.convert(source, ACCOUNT_ID, owner, "crn1", CloudPlatform.GCP.name());
        verify(instanceGroupConverter).convert(any(InstanceGroupRequest.class), anyString(), anyString(), anyString(), anyString(), anyString(),
                mapCaptorForEncryptionKey.capture());
        assertEquals(mapCaptorForEncryptionKey.getValue().get(GCP_KMS_ENCRYPTION_KEY), "dummyEncryptionKey");
    }

    private CreateFreeIpaRequest createCreateFreeIpaRequest() {
        CreateFreeIpaRequest source = new CreateFreeIpaRequest();
        source.setEnvironmentCrn("envCrn");
        source.setName("dummyName");
        source.setAuthentication(new StackAuthenticationRequest());
        source.setTelemetry(new TelemetryRequest());
        source.setInstanceGroups(List.of(new InstanceGroupRequest()));
        FreeIpaServerRequest freeIpaServerRequest = new FreeIpaServerRequest();
        freeIpaServerRequest.setDomain("dummyDomain");
        freeIpaServerRequest.setHostname("dummyHostName");
        source.setFreeIpa(freeIpaServerRequest);

        return source;
    }

    EnumMap<CloudArgsForIgConverter, String> createAndGetCloudArgsForIgMap(String diskEncryptionSetId, String encryptionKey) {
        EnumMap<CloudArgsForIgConverter, String> cloudArgsForIgConverterMap =
                new EnumMap<>(CloudArgsForIgConverter.class);

        cloudArgsForIgConverterMap.put(DISK_ENCRYPTION_SET_ID, diskEncryptionSetId);
        cloudArgsForIgConverterMap.put(GCP_KMS_ENCRYPTION_KEY, encryptionKey);

        return cloudArgsForIgConverterMap;
    }
}