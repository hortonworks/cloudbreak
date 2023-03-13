package com.sequenceiq.cloudbreak.cloud.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsVersionOperations;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseEngine;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Security;

import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.Tag;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AwsStackRequestHelperTest {

    private static final String SSL_CERTIFICATE_IDENTIFIER = "mycert";

    @Mock
    private AwsTaggingService awsTaggingService;

    @Mock
    private AwsCloudFormationClient awsClient;

    @Mock
    private AwsRdsVersionOperations awsRdsVersionOperations;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private DatabaseStack databaseStack;

    @Mock
    private Image image;

    @Mock
    private Network network;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private DatabaseServer databaseServer;

    @Mock
    private Security security;

    @Mock
    private AmazonEc2Client amazonEC2Client;

    @InjectMocks
    private AwsStackRequestHelper underTest;

    static Object[][] testGetStackParametersDbDataProvider() {
        return new Object[][]{
                // testCaseName sslCertificateIdentifier sslCertificateIdentifierParameterDefinedExpected sslCertificateIdentifierParameterExpected
                // engineVersion expectedEngineVersion
                {"sslCertificateIdentifier=null", null, false, null, "10.6", "10.6", "postgres10"},
                {"sslCertificateIdentifier=null, only major version", null, false, null, "10", "10", "postgres10"},
                {"sslCertificateIdentifier=empty", "", false, null, "10.6", "10.6", "postgres10"},
                {"sslCertificateIdentifier=mycert", SSL_CERTIFICATE_IDENTIFIER, true, SSL_CERTIFICATE_IDENTIFIER, "10.6", "10.6", "postgres10"},
        };
    }

    static Object[][] testGetMinimalStackParametersDbDataProvider() {
        return new Object[][]{
                // testCaseName sslCertificateIdentifier
                {"sslCertificateIdentifier=null", null},
                {"sslCertificateIdentifier=empty", ""},
                {"sslCertificateIdentifier=mycert", SSL_CERTIFICATE_IDENTIFIER},
        };
    }

    @BeforeEach
    void setUp() {
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(authenticatedContext.getParameter(any())).thenReturn(amazonEC2Client);

        when(cloudStack.getImage()).thenReturn(image);
        when(cloudStack.getNetwork()).thenReturn(network);
        when(databaseStack.getNetwork()).thenReturn(network);
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);

        when(databaseServer.getSecurity()).thenReturn(security);

        when(awsClient.createEc2Client(any(AwsCredentialView.class), anyString())).thenReturn(amazonEC2Client);
    }

    @Test
    void testCreateCreateStackRequestForCloudStack() {
        when(cloudContext.getLocation()).thenReturn(Location.location(Region.region("region"), new AvailabilityZone("az")));
        DescribeImagesResponse imagesResponse = DescribeImagesResponse.builder()
                .images(software.amazon.awssdk.services.ec2.model.Image.builder().build()).build();
        when(amazonEC2Client.describeImages(any(DescribeImagesRequest.class))).thenReturn(imagesResponse);
        when(network.getStringParameter(anyString())).thenReturn("");

        Collection<Tag> tags = Lists.newArrayList(Tag.builder().key("mytag").value("myvalue").build());
        when(awsTaggingService.prepareCloudformationTags(authenticatedContext, cloudStack.getTags())).thenReturn(tags);

        CreateStackRequest createStackRequest =
                underTest.createCreateStackRequest(authenticatedContext, cloudStack, "stackName", "subnet", "template");

        assertEquals("stackName", createStackRequest.stackName());
        assertEquals("template", createStackRequest.templateBody());

        verify(awsTaggingService).prepareCloudformationTags(authenticatedContext, cloudStack.getTags());
        assertEquals(tags, createStackRequest.tags());
    }

    @Test
    void testCreateCreateStackRequestForDatabaseStack() {
        Collection<Tag> tags = Lists.newArrayList(Tag.builder().key("mytag").value("myvalue").build());
        when(awsTaggingService.prepareCloudformationTags(authenticatedContext, databaseStack.getTags())).thenReturn(tags);

        CreateStackRequest createStackRequest =
                underTest.createCreateStackRequest(authenticatedContext, databaseStack, "stackName", "template");

        assertEquals("stackName", createStackRequest.stackName());
        assertEquals("template", createStackRequest.templateBody());

        verify(awsTaggingService).prepareCloudformationTags(authenticatedContext, cloudStack.getTags());
        assertEquals(tags, createStackRequest.tags());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testGetStackParametersDbDataProvider")
    void testGetStackParametersDb(String testCaseName, String sslCertificateIdentifier, boolean sslCertificateIdentifierParameterDefinedExpected,
            String sslCertificateIdentifierParameterExpected, String engineVersion, String expectedEngineVersion, String expectedFamily) {
        when(network.getStringParameter("subnetId")).thenReturn("subnet-1234");
        when(databaseServer.getEngine()).thenReturn(DatabaseEngine.POSTGRESQL);
        when(awsRdsVersionOperations.getDBParameterGroupFamily(eq(DatabaseEngine.POSTGRESQL), anyString())).thenReturn("postgres10");

        when(databaseServer.getStorageSize()).thenReturn(50L);
        when(databaseServer.getParameter("backupRetentionPeriod", Integer.class)).thenReturn(1);
        when(databaseServer.getFlavor()).thenReturn("db.m3.medium");
        when(databaseServer.getServerId()).thenReturn("myserver");
        when(databaseServer.getEngine()).thenReturn(DatabaseEngine.POSTGRESQL);
        when(databaseServer.getStringParameter("engineVersion")).thenReturn(engineVersion);
        when(databaseServer.getStringParameter("multiAZ")).thenReturn("true");
        when(databaseServer.getStringParameter("storageType")).thenReturn("gp2");
        when(databaseServer.getPort()).thenReturn(5432);
        when(databaseServer.getRootUserName()).thenReturn("root");
        when(databaseServer.getRootPassword()).thenReturn("cloudera");
        when(databaseServer.getSecurity().getCloudSecurityIds()).thenReturn(List.of("sg-1234", "sg-5678"));
        when(databaseServer.isUseSslEnforcement()).thenReturn(true);
        when(databaseServer.getStringParameter(DatabaseServer.SSL_CERTIFICATE_IDENTIFIER)).thenReturn(sslCertificateIdentifier);

        when(cloudContext.getUserName()).thenReturn("bob@cloudera.com");

        Collection<Parameter> parameters = underTest.getStackParameters(authenticatedContext, databaseStack, false);

        assertContainsParameter(parameters, "AllocatedStorageParameter", "50");
        assertContainsParameter(parameters, "BackupRetentionPeriodParameter", "1");
        assertContainsParameter(parameters, "DBInstanceClassParameter", "db.m3.medium");
        assertContainsParameter(parameters, "DBInstanceIdentifierParameter", "myserver");
        assertContainsParameter(parameters, "DBSubnetGroupNameParameter", "dsg-myserver");
        assertContainsParameter(parameters, "DBSubnetGroupSubnetIdsParameter", "subnet-1234");
        assertContainsParameter(parameters, "EngineParameter", "postgres");
        assertContainsParameter(parameters, "EngineVersionParameter", expectedEngineVersion);
        assertContainsParameter(parameters, "MasterUsernameParameter", "root");
        assertContainsParameter(parameters, "MasterUserPasswordParameter", "cloudera");
        assertContainsParameter(parameters, "MultiAZParameter", "true");
        assertContainsParameter(parameters, "PortParameter", "5432");
        assertContainsParameter(parameters, "StorageTypeParameter", "gp2");
        assertContainsParameter(parameters, "VPCSecurityGroupsParameter", "sg-1234,sg-5678");
        assertContainsParameter(parameters, "DeletionProtectionParameter", "false");
        assertContainsParameter(parameters, "DBParameterGroupNameParameter", "dpg-myserver");
        assertContainsParameter(parameters, "DBParameterGroupFamilyParameter", expectedFamily);
        if (sslCertificateIdentifierParameterDefinedExpected) {
            assertContainsParameter(parameters, "SslCertificateIdentifierParameter", sslCertificateIdentifierParameterExpected);
        } else {
            assertDoesNotContainParameter(parameters, "SslCertificateIdentifierParameter");
        }

        parameters = underTest.getStackParameters(authenticatedContext, databaseStack, true);
        assertContainsParameter(parameters, "DeletionProtectionParameter", "true");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testGetMinimalStackParametersDbDataProvider")
    void testGetMinimalStackParametersDb(String testCaseName, String sslCertificateIdentifier) {
        when(network.getStringParameter("subnetId")).thenReturn("subnet-1234");

        when(databaseServer.getStorageSize()).thenReturn(null);
        when(databaseServer.getParameter("backupRetentionPeriod", Integer.class)).thenReturn(null);
        when(databaseServer.getFlavor()).thenReturn("db.m3.medium");
        when(databaseServer.getServerId()).thenReturn("myserver");
        when(databaseServer.getEngine()).thenReturn(DatabaseEngine.POSTGRESQL);
        when(databaseServer.getStringParameter("engineVersion")).thenReturn(null);
        when(databaseServer.getStringParameter("multiAZ")).thenReturn(null);
        when(databaseServer.getStringParameter("storageType")).thenReturn(null);
        when(databaseServer.getPort()).thenReturn(null);
        when(databaseServer.getRootUserName()).thenReturn("root");
        when(databaseServer.getRootPassword()).thenReturn("cloudera");
        when(databaseServer.getSecurity().getCloudSecurityIds()).thenReturn(List.of("sg-1234", "sg-5678"));
        when(databaseServer.isUseSslEnforcement()).thenReturn(false);
        when(databaseServer.getStringParameter(DatabaseServer.SSL_CERTIFICATE_IDENTIFIER)).thenReturn(sslCertificateIdentifier);

        when(cloudContext.getUserName()).thenReturn("bob@cloudera.com");

        Collection<Parameter> parameters = underTest.getStackParameters(authenticatedContext, databaseStack, false);

        assertDoesNotContainParameter(parameters, "AllocatedStorageParameter");
        assertDoesNotContainParameter(parameters, "BackupRetentionPeriodParameter");
        assertDoesNotContainParameter(parameters, "EngineVersionParameter");
        assertDoesNotContainParameter(parameters, "MultiAZParameter");
        assertDoesNotContainParameter(parameters, "StorageTypeParameter");
        assertDoesNotContainParameter(parameters, "PortParameter");
        assertDoesNotContainParameter(parameters, "DBParameterGroupNameParameter");
        assertDoesNotContainParameter(parameters, "DBParameterGroupFamilyParameter");
        assertDoesNotContainParameter(parameters, "SslCertificateIdentifierParameter");
        assertContainsParameter(parameters, "DeletionProtectionParameter", "false");

        parameters = underTest.getStackParameters(authenticatedContext, databaseStack, true);

        assertDoesNotContainParameter(parameters, "AllocatedStorageParameter");
        assertDoesNotContainParameter(parameters, "BackupRetentionPeriodParameter");
        assertDoesNotContainParameter(parameters, "EngineVersionParameter");
        assertDoesNotContainParameter(parameters, "MultiAZParameter");
        assertDoesNotContainParameter(parameters, "StorageTypeParameter");
        assertDoesNotContainParameter(parameters, "PortParameter");
        assertDoesNotContainParameter(parameters, "DBParameterGroupNameParameter");
        assertDoesNotContainParameter(parameters, "DBParameterGroupFamilyParameter");
        assertDoesNotContainParameter(parameters, "SslCertificateIdentifierParameter");
        assertContainsParameter(parameters, "DeletionProtectionParameter", "true");
    }

    @Test
    void testAddParameterChunks() {
        int chunkSize = AwsStackRequestHelper.CHUNK_SIZE;
        String baseParameterKey = RandomStringUtils.randomPrint(20);
        String key1 = baseParameterKey + "1";
        String key2 = baseParameterKey + "2";

        int len = 2 * chunkSize;
        int emptyCount = RandomUtils.nextInt(1, 20);
        String bigParameterValue = RandomStringUtils.randomPrint(chunkSize - emptyCount) +
                StringUtils.repeat(' ', emptyCount) +
                RandomStringUtils.randomPrint(chunkSize);

        Collection<Parameter> parameters = new ArrayList<>();
        underTest.addParameterChunks(parameters, baseParameterKey, bigParameterValue, 3);
        String baseParameterValue = getParameterValue(parameters, baseParameterKey);
        String key1ParameterValue = getParameterValue(parameters, key1);
        assertFalse(Character.isWhitespace(baseParameterValue.charAt(baseParameterValue.length() - 1)));
        assertFalse(Character.isWhitespace(key1ParameterValue.charAt(0)));

        int totalLenght = parameters.stream()
                .map(Parameter::parameterValue)
                .map(String::length)
                .reduce(0, Integer::sum);
        assertEquals(len, totalLenght, "Total chunk parameters length is not equal to the original string length");
        assertEquals(concatParameterValues(parameters, List.of(baseParameterKey, key1, key2)), bigParameterValue,
                "The concatenated value is not equal to the original string");

    }

    @Test
    void testAddParameterChunksEmptyInput() {
        String baseParameterKey = RandomStringUtils.randomPrint(20);
        String key1 = baseParameterKey + "1";
        String key2 = baseParameterKey + "2";

        // len is 0
        Collection<Parameter> parameters = new ArrayList<>();
        underTest.addParameterChunks(parameters, baseParameterKey, "", 3);
        // this is the backwards-compatible behavior
        assertContainsParameter(parameters, baseParameterKey, "");
        assertContainsParameter(parameters, key1, "");
        assertContainsParameter(parameters, key2, "");
    }

    @Test
    void testAddParameterChunksNullInput() {
        String baseParameterKey = RandomStringUtils.randomPrint(20);
        String key1 = baseParameterKey + "1";
        String key2 = baseParameterKey + "2";

        // string is null
        Collection<Parameter> parameters = new ArrayList<>();
        underTest.addParameterChunks(parameters, baseParameterKey, null, 3);
        // this is the backwards-compatible behavior
        assertContainsParameter(parameters, baseParameterKey, null);
        assertContainsParameter(parameters, key1, "");
        assertContainsParameter(parameters, key2, "");
    }

    private String concatParameterValues(Collection<Parameter> parameters, List<String> keys) {
        StringBuilder sb = new StringBuilder();
        keys.forEach(key -> {
            Optional<Parameter> param = parameters.stream()
                    .filter(p -> p.parameterKey().equals(key))
                    .findFirst();
            assertTrue(param.isPresent(), "Parameter is missing with key: " + key);
            sb.append(param.get().parameterValue());
        });
        return sb.toString();
    }

    private String getParameterValue(Collection<Parameter> parameters, String key) {
        Optional<Parameter> foundParameterOpt = parameters.stream()
                .filter(p -> p.parameterKey().equals(key))
                .findFirst();
        assertTrue(foundParameterOpt.isPresent(), "Parameters are missing " + key);
        return foundParameterOpt.get().parameterValue();
    }

    private void assertContainsParameter(Collection<Parameter> parameters, String key, String value) {
        String foundValue = getParameterValue(parameters, key);
        assertEquals(
                value, foundValue, "Parameter " + key + " should have value " + value + " but has value " + foundValue);
    }

    private void assertDoesNotContainParameter(Collection<Parameter> parameters, String key) {
        Optional<Parameter> foundParameterOpt = parameters.stream()
                .filter(p -> p.parameterKey().equals(key))
                .findFirst();
        assertFalse(foundParameterOpt.isPresent(), "Parameters include " + key);
    }

}
