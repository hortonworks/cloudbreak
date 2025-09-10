package com.sequenceiq.redbeams.converter.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.converter.ResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.MappableBase;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParametersBase;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.environment.api.v1.environment.model.response.SecurityAccessResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.SecurityGroupV4StackRequest;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.service.PasswordGeneratorService;
import com.sequenceiq.redbeams.service.UserGeneratorService;

@ExtendWith(MockitoExtension.class)
class DatabaseServerV4StackRequestToDatabaseServerConverterTest {

    private static final String REDBEAMS_DB_MAJOR_VERSION = "10";

    private static final String FIELD_REDBEAMS_DB_MAJOR_VERSION = "redbeamsDbMajorVersion";

    private static final String OWNER_CRN = "crn:cdp:iam:us-west-1:cloudera:user:external/bob@cloudera.com";

    @Mock
    private UserGeneratorService userGeneratorService;

    @Mock
    private PasswordGeneratorService passwordGeneratorService;

    @Mock
    private ResourceNameGenerator nameGenerator;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ProviderParameterCalculator providerParameterCalculator;

    @InjectMocks
    private DatabaseServerV4StackRequestToDatabaseServerConverter underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(underTest, FIELD_REDBEAMS_DB_MAJOR_VERSION, REDBEAMS_DB_MAJOR_VERSION);

    }

    @Test
    void testBuildDatabaseServerWithMinimalRequestOnAzure() {
        // Given
        CloudPlatform cloudPlatform = CloudPlatform.AZURE;
        DatabaseServerV4StackRequest source = new DatabaseServerV4StackRequest();
        source.setInstanceType("Standard_E4ds_v4");
        source.setDatabaseVendor("postgres");
        source.setConnectionDriver("org.postgresql.Driver");
        source.setStorageSize(100L);
        source.setPort(5432);
        setupProviderCalculatorResponse(source, new HashMap<>(Map.of("dbkey", "dbvalue")));

        when(nameGenerator.generateName(APIResourceType.DATABASE_SERVER)).thenReturn("random-uuid");
        when(userGeneratorService.generateUserName()).thenReturn("root");
        when(passwordGeneratorService.generatePassword(any(Optional.class))).thenReturn("random-password");

        // When
        DatabaseServer result = underTest.buildDatabaseServer(source, cloudPlatform);

        // Then
        assertCommonAttributes(result, cloudPlatform);
        assertNull(result.getSecurityGroup());
    }

    @Test
    void testBuildDatabaseServerWithCustomRootUserAndRootPassword() {
        // Given
        CloudPlatform cloudPlatform = CloudPlatform.AWS;
        DatabaseServerV4StackRequest source = new DatabaseServerV4StackRequest();
        source.setInstanceType("db.t2.micro");
        source.setDatabaseVendor("postgres");
        source.setConnectionDriver("org.postgresql.Driver");
        source.setStorageSize(100L);
        source.setRootUserName("myroot");
        source.setRootUserPassword("mysecretpassword");
        source.setPort(5432);
        setupProviderCalculatorResponse(source, new HashMap<>(Map.of("dbkey", "dbvalue")));

        when(nameGenerator.generateName(APIResourceType.DATABASE_SERVER)).thenReturn("random-uuid");

        // When
        DatabaseServer result = underTest.buildDatabaseServer(source, cloudPlatform);

        // Then
        assertCommonAttributes(result, cloudPlatform);
        assertNull(result.getSecurityGroup());
        assertCustomRootUserAndPassword(result);
    }

    @Test
    void testBuildDatabaseServerWithCustomSecurityGroup() {
        // Given
        CloudPlatform cloudPlatform = CloudPlatform.AWS;
        DatabaseServerV4StackRequest source = new DatabaseServerV4StackRequest();
        source.setInstanceType("db.t2.micro");
        source.setDatabaseVendor("postgres");
        source.setConnectionDriver("org.postgresql.Driver");
        source.setStorageSize(100L);
        source.setPort(5432);
        setupProviderCalculatorResponse(source, new HashMap<>(Map.of("dbkey", "dbvalue")));

        SecurityGroupV4StackRequest securityGroupRequest = new SecurityGroupV4StackRequest();
        securityGroupRequest.setSecurityGroupIds(Set.of("sg-12345678"));
        source.setSecurityGroup(securityGroupRequest);

        when(nameGenerator.generateName(APIResourceType.DATABASE_SERVER)).thenReturn("random-uuid");
        when(userGeneratorService.generateUserName()).thenReturn("root");
        when(passwordGeneratorService.generatePassword(any(Optional.class))).thenReturn("random-password");

        // When
        DatabaseServer result = underTest.buildDatabaseServer(source, cloudPlatform);

        // Then
        assertCommonAttributes(result, cloudPlatform);
        assertCustomSecurityGroup(result);
    }

    @Test
    void testBuildDatabaseServerWithCustomSecurityAccessResponse() {
        // Given
        CloudPlatform cloudPlatform = CloudPlatform.AWS;
        DatabaseServerV4StackRequest source = new DatabaseServerV4StackRequest();
        source.setInstanceType("db.t2.micro");
        source.setDatabaseVendor("postgres");
        source.setConnectionDriver("org.postgresql.Driver");
        source.setStorageSize(100L);
        source.setPort(5432);
        setupProviderCalculatorResponse(source, new HashMap<>(Map.of("dbkey", "dbvalue")));

        SecurityAccessResponse securityAccessResponse = SecurityAccessResponse.builder().withDefaultSecurityGroupId("defaultSecurityGroupId").build();

        when(nameGenerator.generateName(APIResourceType.DATABASE_SERVER)).thenReturn("random-uuid");
        when(userGeneratorService.generateUserName()).thenReturn("root");
        when(passwordGeneratorService.generatePassword(any(Optional.class))).thenReturn("random-password");

        // When
        DatabaseServer result = underTest.buildDatabaseServer(source, cloudPlatform, Crn.fromString(OWNER_CRN), securityAccessResponse);

        // Then
        assertCommonAttributes(result, cloudPlatform);
        assertCustomSecurityGroup(result);
        assertEquals("[defaultSecurityGroupId]", result.getSecurityGroup().getSecurityGroupIds().toString());
    }

    private void assertCommonAttributes(DatabaseServer databaseServer, CloudPlatform cloudPlatform) {
        assertNotNull(databaseServer);
        assertNotNull(databaseServer.getName());
        assertNotNull(databaseServer.getInstanceType());
        assertEquals(DatabaseVendor.POSTGRES, databaseServer.getDatabaseVendor());
        assertNotNull(databaseServer.getConnectionDriver());
        assertNotNull(databaseServer.getStorageSize());
        assertNotNull(databaseServer.getRootUserName());
        assertNotNull(databaseServer.getRootPassword());
        assertNotNull(databaseServer.getPort());
        if (cloudPlatform == CloudPlatform.AZURE) {
            assertEquals("10", databaseServer.getAttributes().getString("dbVersion"));
        } else {
            assertEquals("10", databaseServer.getAttributes().getString("engineVersion"));
        }
    }

    private void assertCustomSecurityGroup(DatabaseServer databaseServer) {
        assertNotNull(databaseServer.getSecurityGroup());
        assertNotNull(databaseServer.getSecurityGroup().getSecurityGroupIds());
    }

    private void assertCustomRootUserAndPassword(DatabaseServer databaseServer) {
        assertEquals("myroot", databaseServer.getRootUserName());
        assertEquals("mysecretpassword", databaseServer.getRootPassword());
    }

    private void setupProviderCalculatorResponse(ProviderParametersBase request, Map<String, Object> response) {
        MappableBase providerCalculatorResponse = mock(MappableBase.class);
        when(providerCalculatorResponse.asMap()).thenReturn(response);
        when(providerParameterCalculator.get(request)).thenReturn(providerCalculatorResponse);
    }
}