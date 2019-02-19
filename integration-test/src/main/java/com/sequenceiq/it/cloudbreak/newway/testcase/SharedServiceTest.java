package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;
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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.DirectoryType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests.LdapV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.AdlsCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.location.StorageLocationV4Request;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.RandomNameCreator;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.newway.client.LdapConfigTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.AmbariEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.InstanceGroupEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.blueprint.Blueprint;
import com.sequenceiq.it.cloudbreak.newway.entity.blueprint.BlueprintEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.database.DatabaseEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.ldap.LdapConfigTestDto;

public class SharedServiceTest extends AbstractIntegrationTest {

    private static final String SHARED_SERVICE_TAG = "shared_services_ready";

    private static final String VALID_DL_BP = "{\"Blueprints\":{\"blueprint_name\":\"ownbp\",\"stack_name\":\"HDP\",\"stack_version\":\"2.6\"},\"settings\""
            + ":[{\"recovery_settings\":[]},{\"service_settings\":[]},{\"component_settings\":[]}],\"configurations\":[],\"host_groups\":[{\"name\":\"master\""
            + ",\"configurations\":[],\"components\":[{\"name\":\"HIVE_METASTORE\"},{\"name\":\"HIVE_CLIENT\"},{\"name\":\"RANGER_ADMIN\"}],\"cardinality\":\"1"
            + "\"}]}";

    private static final String HIVE = "hive";

    private static final String RANGER = "ranger";

    private static final String BAD_REQUEST_KEY = "badRequest";

    private static final String RDS_KEY = "rdsConfigNames";

    private static final String LDAP_KEY = "ldapConfigName";

    @Inject
    private LdapConfigTestClient ldapConfigTestClient;

    @Inject
    private RandomNameCreator creator;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        MockedTestContext testContext = (MockedTestContext) data[0];
        minimalSetupForClusterCreation(testContext);
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        ((MockedTestContext) data[0]).cleanupTestContextEntity();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateDatalakeCluster(TestContext testContext) {
        String hiveRdsName = creator.getRandomNameForMock();
        String rangerRdsName = creator.getRandomNameForMock();
        String ldapName = creator.getRandomNameForMock();
        String blueprintName = creator.getRandomNameForMock();
        DatabaseV4Request hiveRds = rdsRequest(DatabaseType.HIVE, hiveRdsName);
        DatabaseV4Request rangerRds = rdsRequest(DatabaseType.RANGER, rangerRdsName);
        testContext
                .given(HIVE, DatabaseEntity.class).withRequest(hiveRds).withName(hiveRdsName)
                .when(DatabaseEntity.post())
                .given(RANGER, DatabaseEntity.class).withRequest(rangerRds).withName(rangerRdsName)
                .when(DatabaseEntity.post())
                .given(LdapConfigTestDto.class).withRequest(ldapRequest(ldapName)).withName(ldapName)
                .when(ldapConfigTestClient.post())
                .given(BlueprintEntity.class).withName(blueprintName).withTag(of(SHARED_SERVICE_TAG), of(true)).withAmbariBlueprint(VALID_DL_BP)
                .when(Blueprint.postV4())
                .given(MASTER.name(), InstanceGroupEntity.class).valid().withHostGroup(MASTER).withNodeCount(1)
                .given(StackTestDto.class)
                .withInstanceGroups(MASTER.name())
                .withCluster(datalakeReadyCluster(testContext, hiveRdsName, rangerRdsName, ldapName, blueprintName, cloudStorage()))
                .capture(SharedServiceTest::rdsConfigNamesFromRequest, key(RDS_KEY))
                .capture(SharedServiceTest::ldapNameFromRequest, key(LDAP_KEY))
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .verify(SharedServiceTest::rdsConfigNamesFromResponse, key(RDS_KEY))
                .verify(SharedServiceTest::ldapNameFromResponse, key(LDAP_KEY))
                .then(SharedServiceTest::checkBlueprintTaggedWithSharedService)
                .then(cloudStorageParametersHasPassedToAmbariBlueprint())
                .then(ldapParametersHasPassedToAmbariBlueprint(ldapName))
                .then(rdsParametersHasPassedToAmbariBlueprint(hiveRds, rangerRds))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateDatalakeClusterWithMoreHostgroupThanSpecifiedInBlueprint(TestContext testContext) {
        String hiveRdsName = creator.getRandomNameForMock();
        String rangerRdsName = creator.getRandomNameForMock();
        String ldapName = creator.getRandomNameForMock();
        String blueprintName = creator.getRandomNameForMock();
        CloudStorageV4Request cloudStorage = cloudStorage();
        testContext
                .given(HIVE, DatabaseEntity.class).valid().withType(DatabaseType.HIVE.name()).withName(hiveRdsName)
                .when(DatabaseEntity.post())
                .given(RANGER, DatabaseEntity.class).valid().withType(DatabaseType.RANGER.name()).withName(rangerRdsName)
                .when(DatabaseEntity.post())
                .given(LdapConfigTestDto.class).withName(ldapName)
                .when(ldapConfigTestClient.post())
                .given(BlueprintEntity.class).withName(blueprintName).withTag(of(SHARED_SERVICE_TAG), of(true)).withAmbariBlueprint(VALID_DL_BP)
                .when(Blueprint.postV4())
                .init(StackTestDto.class)
                .withCluster(datalakeReadyCluster(testContext, hiveRdsName, rangerRdsName, ldapName, blueprintName, cloudStorage))
                .when(Stack.postV4(), key(BAD_REQUEST_KEY))
                .expect(BadRequestException.class, key(BAD_REQUEST_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateDatalakeClusterWithoutLdap(TestContext testContext) {
        String hiveRdsName = creator.getRandomNameForMock();
        String rangerRdsName = creator.getRandomNameForMock();
        String blueprintName = creator.getRandomNameForMock();
        CloudStorageV4Request cloudStorage = cloudStorage();
        testContext
                .given(HIVE, DatabaseEntity.class).valid().withType(DatabaseType.HIVE.name()).withName(hiveRdsName)
                .when(DatabaseEntity.post())
                .given(RANGER, DatabaseEntity.class).valid().withType(DatabaseType.RANGER.name()).withName(rangerRdsName)
                .when(DatabaseEntity.post())
                .given(BlueprintEntity.class).withName(blueprintName).withTag(of(SHARED_SERVICE_TAG), of(true)).withAmbariBlueprint(VALID_DL_BP)
                .when(Blueprint.postV4())
                .given(MASTER.name(), InstanceGroupEntity.class).valid().withHostGroup(MASTER).withNodeCount(1)
                .init(StackTestDto.class)
                .withInstanceGroups(MASTER.name())
                .withCluster(datalakeReadyCluster(testContext, hiveRdsName, rangerRdsName, null, blueprintName, cloudStorage))
                .when(Stack.postV4(), key(BAD_REQUEST_KEY))
                .expect(BadRequestException.class, key(BAD_REQUEST_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateDatalakeClusterWithOnlyOneRdsWhichIsHive(TestContext testContext) {
        String hiveRdsName = creator.getRandomNameForMock();
        String ldapName = creator.getRandomNameForMock();
        String blueprintName = creator.getRandomNameForMock();
        CloudStorageV4Request cloudStorage = cloudStorage();
        testContext
                .given(HIVE, DatabaseEntity.class).valid().withType(DatabaseType.HIVE.name()).withName(hiveRdsName)
                .when(DatabaseEntity.post())
                .given(BlueprintEntity.class).withName(blueprintName).withTag(of(SHARED_SERVICE_TAG), of(true)).withAmbariBlueprint(VALID_DL_BP)
                .when(Blueprint.postV4())
                .given(LdapConfigTestDto.class).withName(ldapName)
                .when(ldapConfigTestClient.post())
                .given(MASTER.name(), InstanceGroupEntity.class).valid().withHostGroup(MASTER).withNodeCount(1)
                .init(StackTestDto.class)
                .withInstanceGroups(MASTER.name())
                .withCluster(datalakeReadyCluster(testContext, hiveRdsName, null, ldapName, blueprintName, cloudStorage))
                .when(Stack.postV4(), key(BAD_REQUEST_KEY))
                .expect(BadRequestException.class, key(BAD_REQUEST_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateDatalakeClusterWithOnlyOneRdsWhichIsRanger(TestContext testContext) {
        String rangerRdsName = creator.getRandomNameForMock();
        String ldapName = creator.getRandomNameForMock();
        String blueprintName = creator.getRandomNameForMock();
        CloudStorageV4Request cloudStorage = cloudStorage();
        testContext
                .given(RANGER, DatabaseEntity.class).valid().withType(DatabaseType.RANGER.name()).withName(rangerRdsName)
                .when(DatabaseEntity.post())
                .given(BlueprintEntity.class).withName(blueprintName).withTag(of(SHARED_SERVICE_TAG), of(true)).withAmbariBlueprint(VALID_DL_BP)
                .when(Blueprint.postV4())
                .given(LdapConfigTestDto.class).withName(ldapName)
                .when(ldapConfigTestClient.post())
                .given(MASTER.name(), InstanceGroupEntity.class).valid().withHostGroup(MASTER).withNodeCount(1)
                .init(StackTestDto.class)
                .withInstanceGroups(MASTER.name())
                .withCluster(datalakeReadyCluster(testContext, null, rangerRdsName, ldapName, blueprintName, cloudStorage))
                .when(Stack.postV4(), key(BAD_REQUEST_KEY))
                .expect(BadRequestException.class, key(BAD_REQUEST_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateDatalakeClusterWithoutRds(TestContext testContext) {
        String ldapName = creator.getRandomNameForMock();
        String blueprintName = creator.getRandomNameForMock();
        CloudStorageV4Request cloudStorage = cloudStorage();
        testContext
                .given(BlueprintEntity.class).withName(blueprintName).withTag(of(SHARED_SERVICE_TAG), of(true)).withAmbariBlueprint(VALID_DL_BP)
                .when(Blueprint.postV4())
                .given(LdapConfigTestDto.class).withName(ldapName)
                .when(ldapConfigTestClient.post())
                .given(MASTER.name(), InstanceGroupEntity.class).valid().withHostGroup(MASTER).withNodeCount(1)
                .init(StackTestDto.class)
                .withInstanceGroups(MASTER.name())
                .withCluster(datalakeReadyCluster(testContext, null, null, ldapName, blueprintName, cloudStorage))
                .when(Stack.postV4(), key(BAD_REQUEST_KEY))
                .expect(BadRequestException.class, key(BAD_REQUEST_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateDatalakeClusterWithoutRdsAndLdap(TestContext testContext) {
        String blueprintName = creator.getRandomNameForMock();
        CloudStorageV4Request cloudStorage = cloudStorage();
        testContext
                .given(BlueprintEntity.class).withName(blueprintName).withTag(of(SHARED_SERVICE_TAG), of(true)).withAmbariBlueprint(VALID_DL_BP)
                .when(Blueprint.postV4())
                .given(MASTER.name(), InstanceGroupEntity.class).valid().withHostGroup(MASTER).withNodeCount(1)
                .init(StackTestDto.class)
                .withInstanceGroups(MASTER.name())
                .withCluster(datalakeReadyCluster(testContext, null, null, null, blueprintName, cloudStorage))
                .when(Stack.postV4(), key(BAD_REQUEST_KEY))
                .expect(BadRequestException.class, key(BAD_REQUEST_KEY))
                .validate();
    }

    private CloudStorageV4Request cloudStorage() {
        AdlsCloudStorageV4Parameters adls = new AdlsCloudStorageV4Parameters();
        CloudStorageV4Request csr = new CloudStorageV4Request();
        csr.setLocations(Set.of(storageLocation()));
        adls.setCredential("value");
        adls.setAccountName("some");
        adls.setClientId("other");
        adls.setTenantId("here");
        csr.setAdls(adls);
        return csr;
    }

    private StorageLocationV4Request storageLocation() {
        StorageLocationV4Request storageLocation = new StorageLocationV4Request();
        storageLocation.setValue("TheValueOfGivePropertyForStorageLocation");
        storageLocation.setPropertyName("NameOfThisProperty");
        storageLocation.setPropertyFile("SomePropertyHere");
        return storageLocation;
    }

    private ClusterEntity datalakeReadyCluster(TestContext testContext, String hiveRdsName, String rangerRdsName, String ldapName, String blueprintName,
            CloudStorageV4Request cloudStorage) {
        ClusterEntity cluster = new ClusterEntity(testContext)
                .valid()
                .withRdsConfigNames(createSetOfNotNulls(hiveRdsName, rangerRdsName))
                .withAmbari(new AmbariEntity(testContext).valid().withBlueprintName(blueprintName));
        if (ldapName != null) {
            cluster.withLdapConfigName(ldapName);
        }
        if (cloudStorage != null) {
            cluster.withCloudStorage(cloudStorage);
        }
        return cluster;
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
        Map<String, Object> blueprintTags = stack.getResponse().getCluster().getAmbari().getBlueprint().getTags();
        if (!blueprintTags.containsKey(SHARED_SERVICE_TAG) || blueprintTags.get(SHARED_SERVICE_TAG) == null
                || !(blueprintTags.get(SHARED_SERVICE_TAG) instanceof Boolean)
                || !((Boolean) blueprintTags.get(SHARED_SERVICE_TAG))) {
            throw new TestFailException("shared service tag has not passed properly (or at all) to the blueprint");
        }
        return stack;
    }

    private List<AssertionV2<StackTestDto>> cloudStorageParametersHasPassedToAmbariBlueprint() {
        List<AssertionV2<StackTestDto>> verifications = new LinkedList<>();
        verifications.add(blueprintPostToAmbariContains(cloudStorage().getAdls().getAccountName()));
        verifications.add(blueprintPostToAmbariContains(cloudStorage().getAdls().getClientId()));
        verifications.add(blueprintPostToAmbariContains(cloudStorage().getAdls().getCredential()));
        return verifications;
    }

    private List<AssertionV2<StackTestDto>> ldapParametersHasPassedToAmbariBlueprint(String ldapName) {
        List<AssertionV2<StackTestDto>> verifications = new LinkedList<>();
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

    private List<AssertionV2<StackTestDto>> rdsParametersHasPassedToAmbariBlueprint(DatabaseV4Request hive, DatabaseV4Request ranger) {
        List<AssertionV2<StackTestDto>> verifications = new LinkedList<>();
        verifications.add(blueprintPostToAmbariContains("ranger_privelege_user_jdbc_url"));
        verifications.add(blueprintPostToAmbariContains(ranger.getConnectionURL()));
        verifications.add(blueprintPostToAmbariContains("javax.jdo.option.ConnectionURL"));
        verifications.add(blueprintPostToAmbariContains(hive.getConnectionURL()));
        verifications.add(blueprintPostToAmbariContains("org.apache.hadoop.fs.adl.AdlFileSystem"));
        return verifications;
    }

    private LdapV4Request ldapRequest(String name) {
        LdapV4Request request = new LdapV4Request();
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
        request.setName(name);
        return request;
    }

    private DatabaseV4Request rdsRequest(DatabaseType type, String name) {
        DatabaseV4Request request = new DatabaseV4Request();
        request.setConnectionURL(format("jdbc:postgresql://somedb.com:5432/%s-mydb", type.name().toLowerCase()));
        request.setConnectionPassword("password");
        request.setConnectionUserName("someuser");
        request.setType(type.name());
        request.setName(name);
        return request;
    }

    private MockVerification blueprintPostToAmbariContains(String content) {
        return MockVerification.verify(HttpMethod.POST, "/api/v1/blueprints/").bodyContains(content);
    }

    private static Set<String> rdsConfigNamesFromResponse(StackTestDto entity) {
        return entity.getResponse().getCluster().getDatabases().stream().map(DatabaseV4Base::getName).collect(Collectors.toSet());
    }

    private static Set<String> rdsConfigNamesFromRequest(StackTestDto entity) {
        return entity.getRequest().getCluster().getDatabases();
    }

    private static String ldapNameFromRequest(StackTestDto entity) {
        return entity.getRequest().getCluster().getLdapName();
    }

    private static String ldapNameFromResponse(StackTestDto entity) {
        return entity.getResponse().getCluster().getLdap().getName();
    }

}