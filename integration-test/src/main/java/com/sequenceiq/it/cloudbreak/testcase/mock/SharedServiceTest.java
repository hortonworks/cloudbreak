package com.sequenceiq.it.cloudbreak.testcase.mock;

import static java.lang.String.format;
import static java.util.List.of;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.springframework.http.HttpMethod;
import org.testng.annotations.Test;

import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.model.CloudStorageCdpService;
import com.sequenceiq.freeipa.api.v1.ldap.model.DirectoryType;
import com.sequenceiq.freeipa.api.v1.ldap.model.create.CreateLdapConfigRequest;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.LdapTestClient;
import com.sequenceiq.it.cloudbreak.client.RedbeamsDatabaseTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseTestDto;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.DatabaseV4Request;

public class SharedServiceTest extends AbstractIntegrationTest {

    private static final String SHARED_SERVICE_TAG = "shared_services_ready";

    private static final String VALID_DL_BP = "{ \"cdhVersion\": \"6.0.99\", \"displayName\": \"datalake\", \"services\": [ { \"refName\": \"zookeeper\", "
            + "\"serviceType\": \"ZOOKEEPER\", \"roleConfigGroups\": [] }, { \"refName\": \"ranger\", \"serviceType\": \"RANGER\", \"serviceConfigs\": [], "
            + "\"roleConfigGroups\": [] }, { \"refName\": \"atlas\", \"serviceType\": \"ATLAS\", \"serviceConfigs\": [], \"roleConfigGroups\": [] }, "
            + "{ \"refName\": \"yarn\", \"serviceType\": \"YARN\", \"roleConfigGroups\": [] }, { \"refName\": \"solr\", \"serviceType\": \"SOLR\", "
            + "\"serviceConfigs\": [], \"roleConfigGroups\": [] }, { \"refName\": \"hbase\", \"serviceType\": \"HBASE\", \"roleConfigGroups\": [] }, "
            + "{ \"refName\": \"hdfs\", \"serviceType\": \"HDFS\", \"roleConfigGroups\": [] }, { \"refName\": \"spark_on_yarn\", \"serviceType\": "
            + "\"SPARK_ON_YARN\", \"serviceConfigs\": [], \"roleConfigGroups\": [] }, { \"refName\": \"kafka\", \"serviceType\": \"KAFKA\", "
            + "\"serviceConfigs\": [], \"roleConfigGroups\": [] }, { \"refName\": \"hive\", \"serviceType\": \"HIVE\", \"roleConfigGroups\": [] } ], "
            + "\"hostTemplates\": [] }";

    private static final String HIVE = "hive";

    private static final String RANGER = "ranger";

    private static final String BAD_REQUEST_KEY = "badRequest";

    private static final String RDS_KEY = "rdsConfigNames";

    private static final String LDAP_KEY = "ldapConfigName";

    @Inject
    private LdapTestClient ldapTestClient;

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private RedbeamsDatabaseTestClient databaseTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    @Description(given = "a datalake cluster", when = "hive rds, ranger rds and ldap are attached", then = "the cluster will be available")
    public void testCreateDatalakeCluster(MockedTestContext testContext) {
        String hiveRdsName = resourcePropertyProvider().getName();
        String rangerRdsName = resourcePropertyProvider().getName();
        String ldapName = resourcePropertyProvider().getName();
        String blueprintName = resourcePropertyProvider().getName();
        DatabaseV4Request hiveRds = rdsRequest("HIVE", hiveRdsName);
        DatabaseV4Request rangerRds = rdsRequest("RANGER", rangerRdsName);
        testContext.getModel().getAmbariMock().postSyncLdap();
        testContext.getModel().getAmbariMock().putConfigureLdap();
        testContext
                .given(HIVE, RedbeamsDatabaseTestDto.class).withRequest(hiveRds).withName(hiveRdsName)
                .when(databaseTestClient.createV4())
                .given(RANGER, RedbeamsDatabaseTestDto.class).withRequest(rangerRds).withName(rangerRdsName)
                .when(databaseTestClient.createV4())
                .given(LdapTestDto.class).withRequest(ldapRequest(ldapName)).withName(ldapName)
                .when(ldapTestClient.createV1())
                .given(BlueprintTestDto.class)
                .withName(blueprintName)
                .withTag(of(SHARED_SERVICE_TAG), of(true))
                .withBlueprint(VALID_DL_BP)
                .when(blueprintTestClient.createV4())
                .given(HostGroupType.MASTER.name(), InstanceGroupTestDto.class).valid().withHostGroup(HostGroupType.MASTER).withNodeCount(1)
                .given(ClusterTestDto.class)
                .withRdsConfigNames(createSetOfNotNulls(hiveRdsName, rangerRdsName))
                .withBlueprintName(blueprintName)
                .withCloudStorage(cloudStorage())
                .given(StackTestDto.class)
                .withInstanceGroups(HostGroupType.MASTER.name())
                .capture(SharedServiceTest::rdsConfigNamesFromRequest, RunningParameter.key(RDS_KEY))
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .verify(SharedServiceTest::rdsConfigNamesFromResponse, RunningParameter.key(RDS_KEY))
                .then(SharedServiceTest::checkBlueprintTaggedWithSharedService)
                .then(cloudStorageParametersHasPassedToAmbariBlueprint())
                .then(ldapParametersHasPassedToAmbariBlueprint(ldapName))
                .then(rdsParametersHasPassedToAmbariBlueprint(hiveRds, rangerRds))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    @Description(given = "a datalake cluster", when = "hive rds, ranger rds and ldap are attached", then = "cluster creation is failed by invalid hostgroups")
    public void testCreateDatalakeClusterWithMoreHostgroupThanSpecifiedInBlueprint(MockedTestContext testContext) {
        String hiveRdsName = resourcePropertyProvider().getName();
        String rangerRdsName = resourcePropertyProvider().getName();
        String ldapName = resourcePropertyProvider().getName();
        String blueprintName = resourcePropertyProvider().getName();
        testContext
                .given(HIVE, RedbeamsDatabaseTestDto.class).valid().withType("HIVE").withName(hiveRdsName)
                .when(databaseTestClient.createV4())
                .given(RANGER, RedbeamsDatabaseTestDto.class).valid().withType("RANGER").withName(rangerRdsName)
                .when(databaseTestClient.createV4())
                .given(LdapTestDto.class).withName(ldapName)
                .when(ldapTestClient.createV1())
                .given(BlueprintTestDto.class)
                .withName(blueprintName)
                .withTag(of(SHARED_SERVICE_TAG), of(true))
                .withBlueprint(VALID_DL_BP)
                .when(blueprintTestClient.createV4())
                .given(ClusterTestDto.class)
                .withRdsConfigNames(createSetOfNotNulls(hiveRdsName, rangerRdsName))
                .withBlueprintName(blueprintName)
                .withCloudStorage(cloudStorage())
                .init(StackTestDto.class)
                .when(stackTestClient.createV4(), RunningParameter.key(BAD_REQUEST_KEY))
                .expect(BadRequestException.class, RunningParameter.key(BAD_REQUEST_KEY).withExpectedMessage("The host groups in the validation \\[master\\] "
                        + "must match the hostgroups in the request \\[compute,worker,master\\]."))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    @Description(given = "a datalake cluster", when = "hive rds, ranger rds are attached", then = "cluster creation is failed by missing ldap")
    public void testCreateDatalakeClusterWithoutLdap(MockedTestContext testContext) {
        String hiveRdsName = resourcePropertyProvider().getName();
        String rangerRdsName = resourcePropertyProvider().getName();
        String blueprintName = resourcePropertyProvider().getName();
        testContext
                .given(HIVE, RedbeamsDatabaseTestDto.class).valid().withType("HIVE").withName(hiveRdsName)
                .when(databaseTestClient.createV4())
                .given(RANGER, RedbeamsDatabaseTestDto.class).valid().withType("RANGER").withName(rangerRdsName)
                .when(databaseTestClient.createV4())
                .given(BlueprintTestDto.class).withName(blueprintName)
                .withTag(of(SHARED_SERVICE_TAG), of(true)).withBlueprint(VALID_DL_BP)
                .when(blueprintTestClient.createV4())
                .given(HostGroupType.MASTER.name(), InstanceGroupTestDto.class).valid().withHostGroup(HostGroupType.MASTER).withNodeCount(1)
                .given(ClusterTestDto.class)
                .withRdsConfigNames(createSetOfNotNulls(hiveRdsName, rangerRdsName))
                .withBlueprintName(blueprintName)
                .withCloudStorage(cloudStorage())
                .init(StackTestDto.class)
                .withInstanceGroups(HostGroupType.MASTER.name())
                .when(stackTestClient.createV4(), RunningParameter.key(BAD_REQUEST_KEY))
                .expect(BadRequestException.class, RunningParameter.key(BAD_REQUEST_KEY).withExpectedMessage("1. For a Datalake cluster \\(since you "
                        + "have selected a datalake "
                        + "ready blueprint\\) you should provide an LDAP configuration or its name/id to the Cluster request"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    @Description(given = "a datalake cluster", when = "hive rds and ldap are attached", then = "cluster creation is failed by missing ranger rds")
    public void testCreateDatalakeClusterWithOnlyOneRdsWhichIsHive(MockedTestContext testContext) {
        String hiveRdsName = resourcePropertyProvider().getName();
        String ldapName = resourcePropertyProvider().getName();
        String blueprintName = resourcePropertyProvider().getName();
        testContext
                .given(HIVE, RedbeamsDatabaseTestDto.class).valid().withType("HIVE").withName(hiveRdsName)
                .when(databaseTestClient.createV4())
                .given(BlueprintTestDto.class).withName(blueprintName)
                .withTag(of(SHARED_SERVICE_TAG), of(true)).withBlueprint(VALID_DL_BP)
                .when(blueprintTestClient.createV4())
                .given(LdapTestDto.class).withName(ldapName)
                .when(ldapTestClient.createV1())
                .given(HostGroupType.MASTER.name(), InstanceGroupTestDto.class).valid().withHostGroup(HostGroupType.MASTER).withNodeCount(1)
                .given(ClusterTestDto.class)
                .withRdsConfigNames(createSetOfNotNulls(hiveRdsName))
                .withBlueprintName(blueprintName)
                .withCloudStorage(cloudStorage())
                .init(StackTestDto.class)
                .withInstanceGroups(HostGroupType.MASTER.name())
                .when(stackTestClient.createV4(), RunningParameter.key(BAD_REQUEST_KEY))
                .expect(BadRequestException.class, RunningParameter.key(BAD_REQUEST_KEY)
                        .withExpectedMessage("1. For a Datalake cluster \\(since you have selected a datalake "
                        + "ready blueprint\\) you should provide at least one Ranger rds/database configuration to the Cluster request"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    @Description(given = "a datalake cluster", when = "ranger rds and ldap are attached", then = "cluster creation is failed by missing hive rds")
    public void testCreateDatalakeClusterWithOnlyOneRdsWhichIsRanger(MockedTestContext testContext) {
        String rangerRdsName = resourcePropertyProvider().getName();
        String ldapName = resourcePropertyProvider().getName();
        String blueprintName = resourcePropertyProvider().getName();
        testContext
                .given(RANGER, RedbeamsDatabaseTestDto.class).valid().withType("RANGER").withName(rangerRdsName)
                .when(databaseTestClient.createV4())
                .given(BlueprintTestDto.class).withName(blueprintName)
                .withTag(of(SHARED_SERVICE_TAG), of(true)).withBlueprint(VALID_DL_BP)
                .when(blueprintTestClient.createV4())
                .given(LdapTestDto.class).withName(ldapName)
                .when(ldapTestClient.createV1())
                .given(HostGroupType.MASTER.name(), InstanceGroupTestDto.class).valid().withHostGroup(HostGroupType.MASTER).withNodeCount(1)
                .given(ClusterTestDto.class)
                .withRdsConfigNames(createSetOfNotNulls(rangerRdsName))
                .withBlueprintName(blueprintName)
                .withCloudStorage(cloudStorage())
                .init(StackTestDto.class)
                .withInstanceGroups(HostGroupType.MASTER.name())
                .when(stackTestClient.createV4(), RunningParameter.key(BAD_REQUEST_KEY))
                .expect(BadRequestException.class, RunningParameter.key(BAD_REQUEST_KEY).withExpectedMessage("1. For a Datalake cluster \\(since you have"
                        + " selected a datalake "
                        + "ready blueprint\\) you should provide at least one Hive rds/database configuration to the Cluster request"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    @Description(given = "a datalake cluster", when = "ldap are attached", then = "cluster creation is failed by missing hive and ranger rds")
    public void testCreateDatalakeClusterWithoutRds(MockedTestContext testContext) {
        String ldapName = resourcePropertyProvider().getName();
        String blueprintName = resourcePropertyProvider().getName();
        testContext
                .given(BlueprintTestDto.class).withName(blueprintName)
                .withTag(of(SHARED_SERVICE_TAG), of(true)).withBlueprint(VALID_DL_BP)
                .when(blueprintTestClient.createV4())
                .given(LdapTestDto.class).withName(ldapName)
                .when(ldapTestClient.createV1())
                .given(HostGroupType.MASTER.name(), InstanceGroupTestDto.class).valid().withHostGroup(HostGroupType.MASTER).withNodeCount(1)
                .given(ClusterTestDto.class)
                .withBlueprintName(blueprintName)
                .withCloudStorage(cloudStorage())
                .init(StackTestDto.class)
                .withInstanceGroups(HostGroupType.MASTER.name())
                .when(stackTestClient.createV4(), RunningParameter.key(BAD_REQUEST_KEY))
                .expect(BadRequestException.class, RunningParameter.key(BAD_REQUEST_KEY).withExpectedMessage("1. For a Datalake cluster \\(since you have"
                        + "selected a datalake "
                        + "ready blueprint\\) you should provide at least one Hive rds/database configuration to the Cluster request\n"
                        + " 2. For a Datalake cluster \\(since you have selected a datalake ready blueprint\\) you should provide at least "
                        + "one Ranger rds/database configuration to the Cluster request"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    @Description(given = "a datalake cluster", when = "without ldap, ranger and hive rds",
            then = "cluster creation is failed by missing hive and ranger rds and ldap")
    public void testCreateDatalakeClusterWithoutRdsAndLdap(MockedTestContext testContext) {
        String blueprintName = resourcePropertyProvider().getName();
        testContext
                .given(BlueprintTestDto.class).withName(blueprintName)
                .withTag(of(SHARED_SERVICE_TAG), of(true)).withBlueprint(VALID_DL_BP)
                .when(blueprintTestClient.createV4())
                .given(HostGroupType.MASTER.name(), InstanceGroupTestDto.class).valid().withHostGroup(HostGroupType.MASTER).withNodeCount(1)
                .given(ClusterTestDto.class)
                .withBlueprintName(blueprintName)
                .withCloudStorage(cloudStorage())
                .init(StackTestDto.class)
                .withInstanceGroups(HostGroupType.MASTER.name())
                .when(stackTestClient.createV4(), RunningParameter.key(BAD_REQUEST_KEY))
                .expect(BadRequestException.class, RunningParameter.key(BAD_REQUEST_KEY).withExpectedMessage("1. For a Datalake cluster \\(since you have"
                        + "selected a datalake "
                        + "ready blueprint\\) you should provide at least one Hive rds/database configuration to the Cluster request\n"
                        + " 2. For a Datalake cluster \\(since you have selected a datalake ready blueprint\\) you should provide at least one "
                        + "Ranger rds/database configuration to the Cluster request\n"
                        + " 3. For a Datalake cluster \\(since you have selected a datalake ready blueprint\\) you should provide an LDAP "
                        + "configuration or its name/id to the Cluster request"))
                .validate();
    }

    private CloudStorageRequest cloudStorage() {
        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();
        cloudStorageRequest.setLocations(List.of(storageLocation()));
        StorageIdentityBase storageIdentityBase = new StorageIdentityBase();
        S3CloudStorageV1Parameters s3Parameters = new S3CloudStorageV1Parameters();
        storageIdentityBase.setS3(s3Parameters);
        cloudStorageRequest.setIdentities(List.of(storageIdentityBase));
        s3Parameters.setInstanceProfile("instanceProfile");
        return cloudStorageRequest;
    }

    private StorageLocationBase storageLocation() {
        StorageLocationBase storageLocation = new StorageLocationBase();
        storageLocation.setValue("TheValueOfGivePropertyForStorageLocation");
        storageLocation.setType(CloudStorageCdpService.RANGER_AUDIT);
        return storageLocation;
    }

    private Set<String> createSetOfNotNulls(String... elements) {
        Set<String> toReturn = new LinkedHashSet<>();
        for (String element : elements) {
            if (element != null) {
                toReturn.add(element);
            }
        }
        return toReturn;
    }

    private static StackTestDto checkBlueprintTaggedWithSharedService(TestContext testContext, StackTestDto stack, CloudbreakClient cloudbreakClient) {
        Map<String, Object> blueprintTags = stack.getResponse().getCluster().getBlueprint().getTags();
        if (!blueprintTags.containsKey(SHARED_SERVICE_TAG) || blueprintTags.get(SHARED_SERVICE_TAG) == null
                || !(blueprintTags.get(SHARED_SERVICE_TAG) instanceof Boolean)
                || !((Boolean) blueprintTags.get(SHARED_SERVICE_TAG))) {
            throw new TestFailException("shared service tag has not passed properly (or at all) to the blueprint");
        }
        return stack;
    }

    private List<Assertion<StackTestDto, CloudbreakClient>> cloudStorageParametersHasPassedToAmbariBlueprint() {
        List<Assertion<StackTestDto, CloudbreakClient>> verifications = new LinkedList<>();
        verifications.add(blueprintPostToAmbariContains(cloudStorage().getIdentities().get(0).getS3().getInstanceProfile()));
        return verifications;
    }

    private List<Assertion<StackTestDto, CloudbreakClient>> ldapParametersHasPassedToAmbariBlueprint(String ldapName) {
        List<Assertion<StackTestDto, CloudbreakClient>> verifications = new LinkedList<>();
        verifications.add(blueprintPostToAmbariContains(ldapRequest(ldapName).getUserDnPattern()));
        verifications.add(blueprintPostToAmbariContains(ldapRequest(ldapName).getBindPassword()));
        verifications.add(blueprintPostToAmbariContains(ldapRequest(ldapName).getBindDn()));
        verifications.add(blueprintPostToAmbariContains(ldapRequest(ldapName).getHost()));
        verifications.add(blueprintPostToAmbariContains(ldapRequest(ldapName).getUserSearchBase()));
        verifications.add(blueprintPostToAmbariContains(ldapRequest(ldapName).getGroupSearchBase()));
        verifications.add(blueprintPostToAmbariContains(ldapRequest(ldapName).getGroupNameAttribute()));
        verifications.add(blueprintPostToAmbariContains(ldapRequest(ldapName).getHost()));
        return verifications;
    }

    private List<Assertion<StackTestDto, CloudbreakClient>> rdsParametersHasPassedToAmbariBlueprint(DatabaseV4Request hive, DatabaseV4Request ranger) {
        List<Assertion<StackTestDto, CloudbreakClient>> verifications = new LinkedList<>();
        verifications.add(blueprintPostToAmbariContains("ranger_privelege_user_jdbc_url"));
        verifications.add(blueprintPostToAmbariContains(ranger.getConnectionURL()));
        verifications.add(blueprintPostToAmbariContains("javax.jdo.option.ConnectionURL"));
        verifications.add(blueprintPostToAmbariContains(hive.getConnectionURL()));
        verifications.add(blueprintPostToAmbariContains("org.apache.hadoop.fs.adl.AdlFileSystem"));
        return verifications;
    }

    private CreateLdapConfigRequest ldapRequest(String environmentCrn) {
        CreateLdapConfigRequest request = new CreateLdapConfigRequest();
        request.setGroupMemberAttribute("memberAttribute");
        request.setUserNameAttribute("userNameAttribute");
        request.setGroupNameAttribute("nameAttribute");
        request.setGroupObjectClass("groupObjectClass");
        request.setGroupSearchBase("groupSearchBase");
        request.setUserObjectClass("userObjectClass");
        request.setDirectoryType(DirectoryType.LDAP);
        request.setUserSearchBase("userSearchBase");
        request.setUserDnPattern("userDnPattern");
        request.setBindPassword("bindPassword");
        request.setDescription("descrition");
        request.setAdminGroup("group");
        request.setHost("host");
        request.setBindDn("bindDn");
        request.setDomain("domain");
        request.setProtocol("http");
        request.setPort(1234);
        request.setName(environmentCrn);
        request.setEnvironmentCrn(environmentCrn);
        return request;
    }

    private DatabaseV4Request rdsRequest(String type, String name) {
        DatabaseV4Request request = new DatabaseV4Request();
        request.setConnectionURL(format("jdbc:postgresql://somedb.com:5432/%s-mydb", type.toLowerCase()));
        request.setConnectionPassword("password");
        request.setConnectionUserName("someuser");
        request.setType(type);
        request.setName(name);
        return request;
    }

    private MockVerification blueprintPostToAmbariContains(String content) {
        return MockVerification.verify(HttpMethod.POST, "/api/v1/blueprints/").bodyContains(content);
    }

    private static Set<String> rdsConfigNamesFromResponse(StackTestDto entity) {
        return entity.getResponse().getCluster().getDatabases().stream().map(String::valueOf).collect(Collectors.toSet());
    }

    private static Set<String> rdsConfigNamesFromRequest(StackTestDto entity) {
        return entity.getRequest().getCluster().getDatabases();
    }
}