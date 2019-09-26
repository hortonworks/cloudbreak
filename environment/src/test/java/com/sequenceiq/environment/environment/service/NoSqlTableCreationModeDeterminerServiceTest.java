package com.sequenceiq.environment.environment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.NoSqlConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableMetadataResponse;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.domain.LocationAwareCredential;
import com.sequenceiq.environment.parameters.dao.domain.S3GuardTableCreation;

@ExtendWith(MockitoExtension.class)
class NoSqlTableCreationModeDeterminerServiceTest {

    @Mock
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CredentialService credentialService;

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private CloudConnector<Object> cloudConnector;

    @Mock
    private NoSqlConnector noSql;

    @InjectMocks
    private NoSqlTableCreationModeDeterminerService underTest;

    @BeforeEach
    void setUp() {
        when(cloudPlatformConnectors.get(any(), any())).thenReturn(cloudConnector);
        when(cloudConnector.noSql()).thenReturn(noSql);
    }

    @Test
    void determineCreationModeExisting() {
        NoSqlTableMetadataResponse metadataResponse = NoSqlTableMetadataResponse.builder()
                .withId("id")
                .withStatus(ResponseStatus.OK)
                .withTableStatus("ACTIVE")
                .build();
        when(noSql.getNoSqlTableMetaData(any())).thenReturn(metadataResponse);
        Credential credential = new Credential();
        credential.setCloudPlatform("platform");
        S3GuardTableCreation mode = underTest.determineCreationMode(LocationAwareCredential.builder()
                .withLocation("location")
                .withCredential(credential)
                .build(), "tablename");
        assertEquals(S3GuardTableCreation.USE_EXISTING, mode);
    }

    @Test
    void determineCreationModeCreateNew() {
        NoSqlTableMetadataResponse metadataResponse = NoSqlTableMetadataResponse.builder()
                .withStatus(ResponseStatus.RESOURCE_NOT_FOUND)
                .build();
        when(noSql.getNoSqlTableMetaData(any())).thenReturn(metadataResponse);
        Credential credential = new Credential();
        credential.setCloudPlatform("platform");
        S3GuardTableCreation mode = underTest.determineCreationMode(LocationAwareCredential.builder()
                .withLocation("location")
                .withCredential(credential)
                .build(), "tablename");
        assertEquals(S3GuardTableCreation.CREATE_NEW, mode);
    }
}
