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

import com.sequenceiq.cloudbreak.api.model.DirectoryType;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigJson;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
import com.sequenceiq.cloudbreak.api.model.v2.CloudStorageRequest;
import com.sequenceiq.cloudbreak.api.model.v2.StorageLocationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.AdlsCloudStorageParameters;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.newway.Blueprint;
import com.sequenceiq.it.cloudbreak.newway.BlueprintEntity;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.LdapConfig;
import com.sequenceiq.it.cloudbreak.newway.LdapConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.RandomNameCreator;
import com.sequenceiq.it.cloudbreak.newway.RdsConfig;
import com.sequenceiq.it.cloudbreak.newway.RdsConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.AmbariEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.InstanceGroupEntity;

public class SharedServiceTest extends AbstractIntegrationTest {

    private static final String TEST_CONTEXT = "testContext";

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
    private RandomNameCreator creator;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        ((TestContext) data[0]).cleanupTestContextEntity();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateDatalakeCluster(TestContext testContext) {
        String hiveRdsName = creator.getRandomNameForMock();
        String rangerRdsName = creator.getRandomNameForMock();
        String ldapName = creator.getRandomNameForMock();
        String blueprintName = creator.getRandomNameForMock();
        RDSConfigRequest hiveRds = rdsRequest(RdsType.HIVE, hiveRdsName);
        RDSConfigRequest rangerRds = rdsRequest(RdsType.RANGER, rangerRdsName);
        testContext
                .given(HIVE, RdsConfigEntity.class).withRequest(hiveRds)
                .when(RdsConfig.postV2())
                .given(RANGER, RdsConfigEntity.class).withRequest(rangerRds)
                .when(RdsConfig.postV2())
                .given(LdapConfigEntity.class).withRequest(ldapRequest(ldapName))
                .when(LdapConfig.postV2())
                .given(BlueprintEntity.class).withName(blueprintName).withTag(of(SHARED_SERVICE_TAG), of(true)).withAmbariBlueprint(VALID_DL_BP)
                .when(Blueprint.postV2())
                .given(MASTER.name(), InstanceGroupEntity.class).valid().withHostGroup(MASTER).withNodeCount(1)
                .given(StackEntity.class)
                .withInstanceGroups(MASTER.name())
                .withCluster(datalakeReadyCluster(testContext, hiveRdsName, rangerRdsName, ldapName, blueprintName, cloudStorage()))
                .capture(SharedServiceTest::rdsConfigNamesFromRequest, key(RDS_KEY))
                .capture(SharedServiceTest::ldapNameFromRequest, key(LDAP_KEY))
                .when(Stack.postV2())
                .await(STACK_AVAILABLE)
                .verify(SharedServiceTest::rdsConfigNamesFromResponse, key(RDS_KEY))
                .verify(SharedServiceTest::ldapNameFromResponse, key(LDAP_KEY))
                .then(SharedServiceTest::checkBlueprintTaggedWithSharedService)
                .then(cloudStorageParametersHasPassedToAmbariBlueprint())
                .then(ldapParametersHasPassedToAmbariBlueprint(ldapName))
                .then(rdsParametersHasPassedToAmbariBlueprint(hiveRds, rangerRds))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateDatalakeClusterWithMoreHostgroupThanSpecifiedInBlueprint(TestContext testContext) {
        String hiveRdsName = creator.getRandomNameForMock();
        String rangerRdsName = creator.getRandomNameForMock();
        String ldapName = creator.getRandomNameForMock();
        String blueprintName = creator.getRandomNameForMock();
        CloudStorageRequest cloudStorage = cloudStorage();
        testContext
                .given(HIVE, RdsConfigEntity.class).valid().withType(RdsType.HIVE.name()).withName(hiveRdsName)
                .when(RdsConfig.postV2())
                .given(RANGER, RdsConfigEntity.class).valid().withType(RdsType.RANGER.name()).withName(rangerRdsName)
                .when(RdsConfig.postV2())
                .given(LdapConfigEntity.class).withName(ldapName)
                .when(LdapConfig.postV2())
                .given(BlueprintEntity.class).withName(blueprintName).withTag(of(SHARED_SERVICE_TAG), of(true)).withAmbariBlueprint(VALID_DL_BP)
                .when(Blueprint.postV2())
                .given(StackEntity.class)
                .withCluster(datalakeReadyCluster(testContext, hiveRdsName, rangerRdsName, ldapName, blueprintName, cloudStorage))
                .when(Stack.postV2(), key(BAD_REQUEST_KEY))
                .except(BadRequestException.class, key(BAD_REQUEST_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateDatalakeClusterWithoutLdap(TestContext testContext) {
        String hiveRdsName = creator.getRandomNameForMock();
        String rangerRdsName = creator.getRandomNameForMock();
        String blueprintName = creator.getRandomNameForMock();
        CloudStorageRequest cloudStorage = cloudStorage();
        testContext
                .given(HIVE, RdsConfigEntity.class).valid().withType(RdsType.HIVE.name()).withName(hiveRdsName)
                .when(RdsConfig.postV2())
                .given(RANGER, RdsConfigEntity.class).valid().withType(RdsType.RANGER.name()).withName(rangerRdsName)
                .when(RdsConfig.postV2())
                .given(BlueprintEntity.class).withName(blueprintName).withTag(of(SHARED_SERVICE_TAG), of(true)).withAmbariBlueprint(VALID_DL_BP)
                .when(Blueprint.postV2())
                .given(MASTER.name(), InstanceGroupEntity.class).valid().withHostGroup(MASTER).withNodeCount(1)
                .given(StackEntity.class)
                .withInstanceGroups(MASTER.name())
                .withCluster(datalakeReadyCluster(testContext, hiveRdsName, rangerRdsName, null, blueprintName, cloudStorage))
                .when(Stack.postV2(), key(BAD_REQUEST_KEY))
                .except(BadRequestException.class, key(BAD_REQUEST_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateDatalakeClusterWithOnlyOneRdsWhichIsHive(TestContext testContext) {
        String hiveRdsName = creator.getRandomNameForMock();
        String ldapName = creator.getRandomNameForMock();
        String blueprintName = creator.getRandomNameForMock();
        CloudStorageRequest cloudStorage = cloudStorage();
        testContext
                .given(HIVE, RdsConfigEntity.class).valid().withType(RdsType.HIVE.name()).withName(hiveRdsName)
                .when(RdsConfig.postV2())
                .given(BlueprintEntity.class).withName(blueprintName).withTag(of(SHARED_SERVICE_TAG), of(true)).withAmbariBlueprint(VALID_DL_BP)
                .when(Blueprint.postV2())
                .given(LdapConfigEntity.class).withName(ldapName)
                .when(LdapConfig.postV2())
                .given(MASTER.name(), InstanceGroupEntity.class).valid().withHostGroup(MASTER).withNodeCount(1)
                .given(StackEntity.class)
                .withInstanceGroups(MASTER.name())
                .withCluster(datalakeReadyCluster(testContext, hiveRdsName, null, ldapName, blueprintName, cloudStorage))
                .when(Stack.postV2(), key(BAD_REQUEST_KEY))
                .except(BadRequestException.class, key(BAD_REQUEST_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateDatalakeClusterWithOnlyOneRdsWhichIsRanger(TestContext testContext) {
        String rangerRdsName = creator.getRandomNameForMock();
        String ldapName = creator.getRandomNameForMock();
        String blueprintName = creator.getRandomNameForMock();
        CloudStorageRequest cloudStorage = cloudStorage();
        testContext
                .given(RANGER, RdsConfigEntity.class).valid().withType(RdsType.RANGER.name()).withName(rangerRdsName)
                .when(RdsConfig.postV2())
                .given(BlueprintEntity.class).withName(blueprintName).withTag(of(SHARED_SERVICE_TAG), of(true)).withAmbariBlueprint(VALID_DL_BP)
                .when(Blueprint.postV2())
                .given(LdapConfigEntity.class).withName(ldapName)
                .when(LdapConfig.postV2())
                .given(MASTER.name(), InstanceGroupEntity.class).valid().withHostGroup(MASTER).withNodeCount(1)
                .given(StackEntity.class)
                .withInstanceGroups(MASTER.name())
                .withCluster(datalakeReadyCluster(testContext, null, rangerRdsName, ldapName, blueprintName, cloudStorage))
                .when(Stack.postV2(), key(BAD_REQUEST_KEY))
                .except(BadRequestException.class, key(BAD_REQUEST_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateDatalakeClusterWithoutRds(TestContext testContext) {
        String ldapName = creator.getRandomNameForMock();
        String blueprintName = creator.getRandomNameForMock();
        CloudStorageRequest cloudStorage = cloudStorage();
        testContext
                .given(BlueprintEntity.class).withName(blueprintName).withTag(of(SHARED_SERVICE_TAG), of(true)).withAmbariBlueprint(VALID_DL_BP)
                .when(Blueprint.postV2())
                .given(LdapConfigEntity.class).withName(ldapName)
                .when(LdapConfig.postV2())
                .given(MASTER.name(), InstanceGroupEntity.class).valid().withHostGroup(MASTER).withNodeCount(1)
                .given(StackEntity.class)
                .withInstanceGroups(MASTER.name())
                .withCluster(datalakeReadyCluster(testContext, null, null, ldapName, blueprintName, cloudStorage))
                .when(Stack.postV2(), key(BAD_REQUEST_KEY))
                .except(BadRequestException.class, key(BAD_REQUEST_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateDatalakeClusterWithoutRdsAndLdap(TestContext testContext) {
        String blueprintName = creator.getRandomNameForMock();
        CloudStorageRequest cloudStorage = cloudStorage();
        testContext
                .given(BlueprintEntity.class).withName(blueprintName).withTag(of(SHARED_SERVICE_TAG), of(true)).withAmbariBlueprint(VALID_DL_BP)
                .when(Blueprint.postV2())
                .given(MASTER.name(), InstanceGroupEntity.class).valid().withHostGroup(MASTER).withNodeCount(1)
                .given(StackEntity.class)
                .withInstanceGroups(MASTER.name())
                .withCluster(datalakeReadyCluster(testContext, null, null, null, blueprintName, cloudStorage))
                .when(Stack.postV2(), key(BAD_REQUEST_KEY))
                .except(BadRequestException.class, key(BAD_REQUEST_KEY))
                .validate();
    }

    private CloudStorageRequest cloudStorage() {
        AdlsCloudStorageParameters adls = new AdlsCloudStorageParameters();
        CloudStorageRequest csr = new CloudStorageRequest();
        csr.setLocations(Set.of(storageLocation()));
        adls.setCredential("value");
        adls.setAccountName("some");
        adls.setClientId("other");
        adls.setTenantId("here");
        csr.setAdls(adls);
        return csr;
    }

    private StorageLocationRequest storageLocation() {
        StorageLocationRequest storageLocation = new StorageLocationRequest();
        storageLocation.setValue("TheValueOfGivePropertyForStorageLocation");
        storageLocation.setPropertyName("NameOfThisProperty");
        storageLocation.setPropertyFile("SomePropertyHere");
        return storageLocation;
    }

    private ClusterEntity datalakeReadyCluster(TestContext testContext, String hiveRdsName, String rangerRdsName, String ldapName, String blueprintName,
                    CloudStorageRequest cloudStorage) {
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

    private static StackEntity checkBlueprintTaggedWithSharedService(TestContext testContext, StackEntity stack, CloudbreakClient cloudbreakClient) {
        Map<String, Object> blueprintTags = stack.getResponse().getCluster().getBlueprint().getTags();
        if (!blueprintTags.containsKey(SHARED_SERVICE_TAG) || blueprintTags.get(SHARED_SERVICE_TAG) == null
                || !(blueprintTags.get(SHARED_SERVICE_TAG) instanceof Boolean)
                || !((Boolean) blueprintTags.get(SHARED_SERVICE_TAG))) {
            throw new TestFailException("shared service tag has not passed properly (or at all) to the blueprint");
        }
        return stack;
    }

    private List<AssertionV2<StackEntity>> cloudStorageParametersHasPassedToAmbariBlueprint() {
        List<AssertionV2<StackEntity>> verifications = new LinkedList<>();
        verifications.add(blueprintPostToAmbariContains(cloudStorage().getAdls().getAccountName()));
        verifications.add(blueprintPostToAmbariContains(cloudStorage().getAdls().getClientId()));
        verifications.add(blueprintPostToAmbariContains(cloudStorage().getAdls().getCredential()));
        return verifications;
    }

    private List<AssertionV2<StackEntity>> ldapParametersHasPassedToAmbariBlueprint(String ldapName) {
        List<AssertionV2<StackEntity>> verifications = new LinkedList<>();
        verifications.add(blueprintPostToAmbariContains(ldapRequest(ldapName).getUserDnPattern()));
        verifications.add(blueprintPostToAmbariContains(ldapRequest(ldapName).getBindPassword()));
        verifications.add(blueprintPostToAmbariContains(ldapRequest(ldapName).getBindDn()));
        verifications.add(blueprintPostToAmbariContains(ldapRequest(ldapName).getServerHost()));
        verifications.add(blueprintPostToAmbariContains(ldapRequest(ldapName).getUserSearchBase()));
        verifications.add(blueprintPostToAmbariContains(ldapRequest(ldapName).getGroupSearchBase()));
        verifications.add(blueprintPostToAmbariContains(ldapRequest(ldapName).getGroupNameAttribute()));
        verifications.add(blueprintPostToAmbariContains(ldapRequest(ldapName).getServerHost()));
        return verifications;
    }

    private List<AssertionV2<StackEntity>> rdsParametersHasPassedToAmbariBlueprint(RDSConfigRequest hive, RDSConfigRequest ranger) {
        List<AssertionV2<StackEntity>> verifications = new LinkedList<>();
        verifications.add(blueprintPostToAmbariContains("ranger_privelege_user_jdbc_url"));
        verifications.add(blueprintPostToAmbariContains(ranger.getConnectionURL()));
        verifications.add(blueprintPostToAmbariContains("javax.jdo.option.ConnectionURL"));
        verifications.add(blueprintPostToAmbariContains(hive.getConnectionURL()));
        verifications.add(blueprintPostToAmbariContains("org.apache.hadoop.fs.adl.AdlFileSystem"));
        return verifications;
    }

    private LdapConfigRequest ldapRequest(String name) {
        LdapConfigRequest request = new LdapConfigRequest();
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
        request.setServerHost("host");
        request.setBindDn("bindDn");
        request.setDomain("domain");
        request.setProtocol("http");
        request.setServerPort(1234);
        request.setName(name);
        return request;
    }

    private RDSConfigRequest rdsRequest(RdsType type, String name) {
        RDSConfigRequest request = new RDSConfigRequest();
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

    private static Set<String> rdsConfigNamesFromResponse(StackEntity entity) {
        return entity.getResponse().getCluster().getRdsConfigs().stream().map(RDSConfigJson::getName).collect(Collectors.toSet());
    }

    private static Set<String> rdsConfigNamesFromRequest(StackEntity entity) {
        return entity.getRequest().getCluster().getRdsConfigNames();
    }

    private static String ldapNameFromRequest(StackEntity entity) {
        return entity.getRequest().getCluster().getLdapConfigName();
    }

    private static String ldapNameFromResponse(StackEntity entity) {
        return entity.getResponse().getCluster().getLdapConfig().getName();
    }

}