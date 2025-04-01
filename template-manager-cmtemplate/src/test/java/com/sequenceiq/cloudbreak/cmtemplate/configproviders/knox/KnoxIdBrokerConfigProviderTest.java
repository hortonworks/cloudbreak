package com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigTestUtil.getConfigNameToValueMap;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigTestUtil.getConfigNameToVariableNameMap;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox.KnoxRoles.IDBROKER;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox.KnoxRoles.KNOX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.wasb.WasbFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.views.AccountMappingView;

public class KnoxIdBrokerConfigProviderTest {

    // Note: We need a predictable iteration order, so cannot rely on Map.ofEntries()
    private static final Map<String, String> USER_MAPPINGS = fixedIterationMapOfEntries(
            Map.entry("user1", "role1"),
            Map.entry("user2", "role2"),
            Map.entry("user3", "role3")
    );

    private static final Map<String, String> GROUP_MAPPINGS = fixedIterationMapOfEntries(
            Map.entry("group4", "role4"),
            Map.entry("group5", "role5")
    );

    private static final String USER_MAPPINGS_STR = "user1=role1;user2=role2;user3=role3";

    private static final String GROUP_MAPPINGS_STR = "group4=role4;group5=role5";

    private static final String MANAGED_IDENTITY_STR = "managedIdentity";

    private static final String IDBROKER_AWS_USER_MAPPING = "idbroker_aws_user_mapping";

    private static final String IDBROKER_AWS_GROUP_MAPPING = "idbroker_aws_group_mapping";

    private static final String IDBROKER_AZURE_USER_MAPPING = "idbroker_azure_user_mapping";

    private static final String IDBROKER_AZURE_GROUP_MAPPING = "idbroker_azure_group_mapping";

    private static final String IDBROKER_AZURE_VM_ASSUMER_IDENTITY = "idbroker_azure_vm_assumer_identity";

    private static final String IDBROKER_GCP_USER_MAPPING = "idbroker_gcp_user_mapping";

    private static final String IDBROKER_GCP_GROUP_MAPPING = "idbroker_gcp_group_mapping";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private CmTemplateProcessor cmTemplate;

    @InjectMocks
    private KnoxIdBrokerConfigProvider underTest;

    private static <K, V> Map<K, V> fixedIterationMapOfEntries(Map.Entry<K, V>... entries) {
        Map<K, V> result = new LinkedHashMap<>();
        Arrays.stream(entries).forEach(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getRoleConfigWhenBadRole() {
        TemplatePreparationObject tpo = new Builder()
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfigs("DUMMY", cmTemplate, tpo);

        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).isEmpty();
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getRoleConfigWhenIdBrokerAndNoFileSystemAndAwsAndNoMappings() {
        TemplatePreparationObject tpo = new Builder()
                .withCloudPlatform(CloudPlatform.AWS)
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfigs(IDBROKER, cmTemplate, tpo);

        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                Map.entry(IDBROKER_AWS_USER_MAPPING, ""),
                Map.entry(IDBROKER_AWS_GROUP_MAPPING, "")
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getRoleConfigWhenIdBrokerAndNoFileSystemAndAws() {
        TemplatePreparationObject tpo = new Builder()
                .withCloudPlatform(CloudPlatform.AWS)
                .withAccountMappingView(new AccountMappingView(GROUP_MAPPINGS, USER_MAPPINGS))
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfigs(IDBROKER, cmTemplate, tpo);

        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                Map.entry(IDBROKER_AWS_USER_MAPPING, USER_MAPPINGS_STR),
                Map.entry(IDBROKER_AWS_GROUP_MAPPING, GROUP_MAPPINGS_STR)
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getRoleConfigWhenIdBrokerAndNoFileSystemAndAzure() {
        TemplatePreparationObject tpo = new Builder()
                .withCloudPlatform(CloudPlatform.AZURE)
                .withAccountMappingView(new AccountMappingView(GROUP_MAPPINGS, USER_MAPPINGS))
                .withProductDetails(generateCMRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0), null)
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfigs(IDBROKER, cmTemplate, tpo);

        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                Map.entry(IDBROKER_AZURE_USER_MAPPING, USER_MAPPINGS_STR),
                Map.entry(IDBROKER_AZURE_GROUP_MAPPING, GROUP_MAPPINGS_STR)
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getRoleConfigWhenIdBrokerAndNoFileSystemAndGcp() {
        TemplatePreparationObject tpo = new Builder()
                .withCloudPlatform(CloudPlatform.GCP)
                .withAccountMappingView(new AccountMappingView(GROUP_MAPPINGS, USER_MAPPINGS))
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfigs(IDBROKER, cmTemplate, tpo);

        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                Map.entry(IDBROKER_GCP_USER_MAPPING, USER_MAPPINGS_STR),
                Map.entry(IDBROKER_GCP_GROUP_MAPPING, GROUP_MAPPINGS_STR)
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getRoleConfigWhenIdBrokerAndNoFileSystemAndYarn() {
        TemplatePreparationObject tpo = new Builder()
                .withCloudPlatform(CloudPlatform.YARN)
                .withAccountMappingView(new AccountMappingView(GROUP_MAPPINGS, USER_MAPPINGS))
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfigs(IDBROKER, cmTemplate, tpo);

        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).isEmpty();
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getRoleConfigWhenIdBrokerAndInvalidFileSystem() {
        BaseFileSystemConfigurationsView fileSystemConfigurationsView = mock(BaseFileSystemConfigurationsView.class);
        when(fileSystemConfigurationsView.getType()).thenReturn("DOS");

        TemplatePreparationObject tpo = new Builder()
                .withCloudPlatform(CloudPlatform.AWS)
                .withFileSystemConfigurationView(fileSystemConfigurationsView)
                .build();

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Unknown file system type:");

        underTest.getRoleConfigs(IDBROKER, cmTemplate, tpo);
    }

    @Test
    public void getRoleConfigWhenIdBrokerAndS3FileSystem() {
        BaseFileSystemConfigurationsView fileSystemConfigurationsView = mock(BaseFileSystemConfigurationsView.class);
        when(fileSystemConfigurationsView.getType()).thenReturn("S3");

        TemplatePreparationObject tpo = new Builder()
                .withCloudPlatform(CloudPlatform.AWS)
                .withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withAccountMappingView(new AccountMappingView(GROUP_MAPPINGS, USER_MAPPINGS))
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfigs(IDBROKER, cmTemplate, tpo);

        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                Map.entry(IDBROKER_AWS_USER_MAPPING, USER_MAPPINGS_STR),
                Map.entry(IDBROKER_AWS_GROUP_MAPPING, GROUP_MAPPINGS_STR)
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getRoleConfigWhenIdBrokerAndAdlsFileSystem() {
        BaseFileSystemConfigurationsView fileSystemConfigurationsView = mock(BaseFileSystemConfigurationsView.class);
        when(fileSystemConfigurationsView.getType()).thenReturn("ADLS");

        TemplatePreparationObject tpo = new Builder()
                .withCloudPlatform(CloudPlatform.AZURE)
                .withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withAccountMappingView(new AccountMappingView(GROUP_MAPPINGS, USER_MAPPINGS))
                .withProductDetails(generateCMRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0), null)
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfigs(IDBROKER, cmTemplate, tpo);

        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                Map.entry(IDBROKER_AZURE_USER_MAPPING, USER_MAPPINGS_STR),
                Map.entry(IDBROKER_AZURE_GROUP_MAPPING, GROUP_MAPPINGS_STR)
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getRoleConfigWhenIdBrokerAndAdlsGen2FileSystemAndValidCMVersionValidEntity() {
        BaseFileSystemConfigurationsView fileSystemConfigurationsView = mock(BaseFileSystemConfigurationsView.class);
        when(fileSystemConfigurationsView.getType()).thenReturn("ADLS_GEN_2");
        when(fileSystemConfigurationsView.getIdBrokerIdentityId()).thenReturn(MANAGED_IDENTITY_STR);


        TemplatePreparationObject tpo = new Builder()
                .withCloudPlatform(CloudPlatform.AZURE)
                .withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withAccountMappingView(new AccountMappingView(GROUP_MAPPINGS, USER_MAPPINGS))
                .withProductDetails(generateCMRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0), null)
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfigs(IDBROKER, cmTemplate, tpo);

        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                Map.entry(IDBROKER_AZURE_USER_MAPPING, USER_MAPPINGS_STR),
                Map.entry(IDBROKER_AZURE_GROUP_MAPPING, GROUP_MAPPINGS_STR),
                Map.entry(IDBROKER_AZURE_VM_ASSUMER_IDENTITY, MANAGED_IDENTITY_STR)
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getRoleConfigWhenIdBrokerAndAdlsGen2FileSystemAndInvalidCMVersion() {
        BaseFileSystemConfigurationsView fileSystemConfigurationsView = mock(BaseFileSystemConfigurationsView.class);
        when(fileSystemConfigurationsView.getType()).thenReturn("ADLS_GEN_2");

        TemplatePreparationObject tpo = new Builder()
                .withCloudPlatform(CloudPlatform.AZURE)
                .withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withAccountMappingView(new AccountMappingView(GROUP_MAPPINGS, USER_MAPPINGS))
                .withProductDetails(generateCMRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_6_3_0), null)
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfigs(IDBROKER, cmTemplate, tpo);

        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                Map.entry(IDBROKER_AZURE_USER_MAPPING, USER_MAPPINGS_STR),
                Map.entry(IDBROKER_AZURE_GROUP_MAPPING, GROUP_MAPPINGS_STR)
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getRoleConfigWhenIdBrokerAndAdlsGen2FileSystemAndValidCMVersionInvalidEntity() {
        BaseFileSystemConfigurationsView fileSystemConfigurationsView = mock(BaseFileSystemConfigurationsView.class);
        when(fileSystemConfigurationsView.getType()).thenReturn("ADLS_GEN_2");

        TemplatePreparationObject tpo = new Builder()
                .withCloudPlatform(CloudPlatform.AZURE)
                .withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withAccountMappingView(new AccountMappingView(GROUP_MAPPINGS, USER_MAPPINGS))
                .withProductDetails(generateCMRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0), null)
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfigs(IDBROKER, cmTemplate, tpo);

        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                Map.entry(IDBROKER_AZURE_USER_MAPPING, USER_MAPPINGS_STR),
                Map.entry(IDBROKER_AZURE_GROUP_MAPPING, GROUP_MAPPINGS_STR)
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getRoleConfigWhenIdBrokerAndWasbFileSystem() {
        WasbFileSystemConfigurationsView fileSystemConfigurationsView = mock(WasbFileSystemConfigurationsView.class);
        when(fileSystemConfigurationsView.getType()).thenReturn("WASB");

        TemplatePreparationObject tpo = new Builder()
                .withCloudPlatform(CloudPlatform.AZURE)
                .withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withAccountMappingView(new AccountMappingView(GROUP_MAPPINGS, USER_MAPPINGS))
                .withProductDetails(generateCMRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0), null)
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfigs(IDBROKER, cmTemplate, tpo);

        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                Map.entry(IDBROKER_AZURE_USER_MAPPING, USER_MAPPINGS_STR),
                Map.entry(IDBROKER_AZURE_GROUP_MAPPING, GROUP_MAPPINGS_STR)
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getRoleConfigWhenIdBrokerAndWasbIntegratedFileSystem() {
        BaseFileSystemConfigurationsView fileSystemConfigurationsView = mock(BaseFileSystemConfigurationsView.class);
        when(fileSystemConfigurationsView.getType()).thenReturn("WASB_INTEGRATED");

        TemplatePreparationObject tpo = new Builder()
                .withCloudPlatform(CloudPlatform.AZURE)
                .withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withAccountMappingView(new AccountMappingView(GROUP_MAPPINGS, USER_MAPPINGS))
                .withProductDetails(generateCMRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0), null)
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfigs(IDBROKER, cmTemplate, tpo);

        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                Map.entry(IDBROKER_AZURE_USER_MAPPING, USER_MAPPINGS_STR),
                Map.entry(IDBROKER_AZURE_GROUP_MAPPING, GROUP_MAPPINGS_STR)
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getRoleConfigWhenIdBrokerAndGcsFileSystem() {
        BaseFileSystemConfigurationsView fileSystemConfigurationsView = mock(BaseFileSystemConfigurationsView.class);
        when(fileSystemConfigurationsView.getType()).thenReturn("GCS");

        TemplatePreparationObject tpo = new Builder()
                .withCloudPlatform(CloudPlatform.GCP)
                .withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withAccountMappingView(new AccountMappingView(GROUP_MAPPINGS, USER_MAPPINGS))
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfigs(IDBROKER, cmTemplate, tpo);

        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                Map.entry(IDBROKER_GCP_USER_MAPPING, USER_MAPPINGS_STR),
                Map.entry(IDBROKER_GCP_GROUP_MAPPING, GROUP_MAPPINGS_STR)
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getServiceType() {
        assertThat(underTest.getServiceType()).isEqualTo(KNOX);
    }

    @Test
    public void getRoleTypes() {
        assertThat(underTest.getRoleTypes()).containsOnly(IDBROKER);
    }

    private ClouderaManagerRepo generateCMRepo(Versioned version) {
        return new ClouderaManagerRepo()
                .withBaseUrl("baseurl")
                .withGpgKeyUrl("gpgurl")
                .withPredefined(true)
                .withVersion(version.getVersion());
    }

}
