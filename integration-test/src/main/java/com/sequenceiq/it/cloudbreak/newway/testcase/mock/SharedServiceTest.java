package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

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
import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.newway.client.ClusterDefinitionTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.DatabaseTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.LdapTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.AmbariTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.clusterdefinition.ClusterDefinitionTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.database.DatabaseTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

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
    private LdapTestClient ldapTestClient;

    @Inject
    private ClusterDefinitionTestClient clusterDefinitionTestClient;

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private DatabaseTestClient databaseTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(given = "a datalake cluster", when = "hive rds, ranger rds and ldap are attached", then = "the cluster will be available")
    public void testCreateDatalakeCluster(MockedTestContext testContext) {
        String hiveRdsName = resourcePropertyProvider().getName();
        String rangerRdsName = resourcePropertyProvider().getName();
        String ldapName = resourcePropertyProvider().getName();
        String clusterDefinitionName = resourcePropertyProvider().getName();
        DatabaseV4Request hiveRds = rdsRequest(DatabaseType.HIVE, hiveRdsName);
        DatabaseV4Request rangerRds = rdsRequest(DatabaseType.RANGER, rangerRdsName);
        testContext.getModel().getAmbariMock().postSyncLdap();
        testContext.getModel().getAmbariMock().putConfigureLdap();
        testContext
                .given(HIVE, DatabaseTestDto.class).withRequest(hiveRds).withName(hiveRdsName)
                .when(databaseTestClient.createV4())
                .given(RANGER, DatabaseTestDto.class).withRequest(rangerRds).withName(rangerRdsName)
                .when(databaseTestClient.createV4())
                .given(LdapTestDto.class).withRequest(ldapRequest(ldapName)).withName(ldapName)
                .when(ldapTestClient.createV4())
                .given(ClusterDefinitionTestDto.class)
                .withName(clusterDefinitionName)
                .withTag(of(SHARED_SERVICE_TAG), of(true))
                .withClusterDefinition(VALID_DL_BP)
                .when(clusterDefinitionTestClient.createV4())
                .given(MASTER.name(), InstanceGroupTestDto.class).valid().withHostGroup(MASTER).withNodeCount(1)
                .given(ClusterTestDto.class)
                .withRdsConfigNames(createSetOfNotNulls(hiveRdsName, rangerRdsName))
                .withClusterDefinitionName(clusterDefinitionName)
                .withAmbari(testContext.given(AmbariTestDto.class))
                .withLdapConfigName(ldapName)
                .withCloudStorage(cloudStorage())
                .given(StackTestDto.class)
                .withInstanceGroups(MASTER.name())
                .capture(SharedServiceTest::rdsConfigNamesFromRequest, key(RDS_KEY))
                .capture(SharedServiceTest::ldapNameFromRequest, key(LDAP_KEY))
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .verify(SharedServiceTest::rdsConfigNamesFromResponse, key(RDS_KEY))
                .verify(SharedServiceTest::ldapNameFromResponse, key(LDAP_KEY))
                .then(SharedServiceTest::checkClusterDefinitionTaggedWithSharedService)
                .then(cloudStorageParametersHasPassedToAmbariClusterDefinition())
                .then(ldapParametersHasPassedToAmbariClusterDefinition(ldapName))
                .then(rdsParametersHasPassedToAmbariClusterDefinition(hiveRds, rangerRds))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(given = "a datalake cluster", when = "hive rds, ranger rds and ldap are attached", then = "cluster creation is failed by invalid hostgroups")
    public void testCreateDatalakeClusterWithMoreHostgroupThanSpecifiedInClusterDefinition(TestContext testContext) {
        String hiveRdsName = resourcePropertyProvider().getName();
        String rangerRdsName = resourcePropertyProvider().getName();
        String ldapName = resourcePropertyProvider().getName();
        String clusterDefinitionName = resourcePropertyProvider().getName();
        testContext
                .given(HIVE, DatabaseTestDto.class).valid().withType(DatabaseType.HIVE.name()).withName(hiveRdsName)
                .when(databaseTestClient.createV4())
                .given(RANGER, DatabaseTestDto.class).valid().withType(DatabaseType.RANGER.name()).withName(rangerRdsName)
                .when(databaseTestClient.createV4())
                .given(LdapTestDto.class).withName(ldapName)
                .when(ldapTestClient.createV4())
                .given(ClusterDefinitionTestDto.class)
                .withName(clusterDefinitionName)
                .withTag(of(SHARED_SERVICE_TAG), of(true))
                .withClusterDefinition(VALID_DL_BP)
                .when(clusterDefinitionTestClient.createV4())
                .given(ClusterTestDto.class)
                .withRdsConfigNames(createSetOfNotNulls(hiveRdsName, rangerRdsName))
                .withClusterDefinitionName(clusterDefinitionName)
                .withAmbari(testContext.given(AmbariTestDto.class))
                .withLdapConfigName(ldapName)
                .withCloudStorage(cloudStorage())
                .init(StackTestDto.class)
                .when(stackTestClient.createV4(), key(BAD_REQUEST_KEY))
                .expect(BadRequestException.class, key(BAD_REQUEST_KEY).withExpectedMessage("The host groups in the validation \\[master\\] "
                        + "must match the hostgroups in the request \\[compute,worker,master\\]."))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(given = "a datalake cluster", when = "hive rds, ranger rds are attached", then = "cluster creation is failed by missing ldap")
    public void testCreateDatalakeClusterWithoutLdap(TestContext testContext) {
        String hiveRdsName = resourcePropertyProvider().getName();
        String rangerRdsName = resourcePropertyProvider().getName();
        String clusterDefinitionName = resourcePropertyProvider().getName();
        testContext
                .given(HIVE, DatabaseTestDto.class).valid().withType(DatabaseType.HIVE.name()).withName(hiveRdsName)
                .when(databaseTestClient.createV4())
                .given(RANGER, DatabaseTestDto.class).valid().withType(DatabaseType.RANGER.name()).withName(rangerRdsName)
                .when(databaseTestClient.createV4())
                .given(ClusterDefinitionTestDto.class).withName(clusterDefinitionName)
                .withTag(of(SHARED_SERVICE_TAG), of(true)).withClusterDefinition(VALID_DL_BP)
                .when(clusterDefinitionTestClient.createV4())
                .given(MASTER.name(), InstanceGroupTestDto.class).valid().withHostGroup(MASTER).withNodeCount(1)
                .given(ClusterTestDto.class)
                .withRdsConfigNames(createSetOfNotNulls(hiveRdsName, rangerRdsName))
                .withClusterDefinitionName(clusterDefinitionName)
                .withAmbari(testContext.given(AmbariTestDto.class))
                .withCloudStorage(cloudStorage())
                .init(StackTestDto.class)
                .withInstanceGroups(MASTER.name())
                .when(stackTestClient.createV4(), key(BAD_REQUEST_KEY))
                .expect(BadRequestException.class, key(BAD_REQUEST_KEY).withExpectedMessage("1. For a Datalake cluster \\(since you have selected a datalake "
                        + "ready cluster definition\\) you should provide an LDAP configuration or its name/id to the Cluster request"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(given = "a datalake cluster", when = "hive rds and ldap are attached", then = "cluster creation is failed by missing ranger rds")
    public void testCreateDatalakeClusterWithOnlyOneRdsWhichIsHive(TestContext testContext) {
        String hiveRdsName = resourcePropertyProvider().getName();
        String ldapName = resourcePropertyProvider().getName();
        String clusterDefinitionName = resourcePropertyProvider().getName();
        testContext
                .given(HIVE, DatabaseTestDto.class).valid().withType(DatabaseType.HIVE.name()).withName(hiveRdsName)
                .when(databaseTestClient.createV4())
                .given(ClusterDefinitionTestDto.class).withName(clusterDefinitionName)
                .withTag(of(SHARED_SERVICE_TAG), of(true)).withClusterDefinition(VALID_DL_BP)
                .when(clusterDefinitionTestClient.createV4())
                .given(LdapTestDto.class).withName(ldapName)
                .when(ldapTestClient.createV4())
                .given(MASTER.name(), InstanceGroupTestDto.class).valid().withHostGroup(MASTER).withNodeCount(1)
                .given(ClusterTestDto.class)
                .withRdsConfigNames(createSetOfNotNulls(hiveRdsName))
                .withClusterDefinitionName(clusterDefinitionName)
                .withAmbari(testContext.given(AmbariTestDto.class))
                .withLdapConfigName(ldapName)
                .withCloudStorage(cloudStorage())
                .init(StackTestDto.class)
                .withInstanceGroups(MASTER.name())
                .when(stackTestClient.createV4(), key(BAD_REQUEST_KEY))
                .expect(BadRequestException.class, key(BAD_REQUEST_KEY).withExpectedMessage("1. For a Datalake cluster \\(since you have selected a datalake "
                        + "ready cluster definition\\) you should provide at least one Ranger rds/database configuration to the Cluster request"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(given = "a datalake cluster", when = "ranger rds and ldap are attached", then = "cluster creation is failed by missing hive rds")
    public void testCreateDatalakeClusterWithOnlyOneRdsWhichIsRanger(TestContext testContext) {
        String rangerRdsName = resourcePropertyProvider().getName();
        String ldapName = resourcePropertyProvider().getName();
        String clusterDefinitionName = resourcePropertyProvider().getName();
        testContext
                .given(RANGER, DatabaseTestDto.class).valid().withType(DatabaseType.RANGER.name()).withName(rangerRdsName)
                .when(databaseTestClient.createV4())
                .given(ClusterDefinitionTestDto.class).withName(clusterDefinitionName)
                .withTag(of(SHARED_SERVICE_TAG), of(true)).withClusterDefinition(VALID_DL_BP)
                .when(clusterDefinitionTestClient.createV4())
                .given(LdapTestDto.class).withName(ldapName)
                .when(ldapTestClient.createV4())
                .given(MASTER.name(), InstanceGroupTestDto.class).valid().withHostGroup(MASTER).withNodeCount(1)
                .given(ClusterTestDto.class)
                .withRdsConfigNames(createSetOfNotNulls(rangerRdsName))
                .withClusterDefinitionName(clusterDefinitionName)
                .withAmbari(testContext.given(AmbariTestDto.class))
                .withLdapConfigName(ldapName)
                .withCloudStorage(cloudStorage())
                .init(StackTestDto.class)
                .withInstanceGroups(MASTER.name())
                .when(stackTestClient.createV4(), key(BAD_REQUEST_KEY))
                .expect(BadRequestException.class, key(BAD_REQUEST_KEY).withExpectedMessage("1. For a Datalake cluster \\(since you have selected a datalake "
                        + "ready cluster definition\\) you should provide at least one Hive rds/database configuration to the Cluster request"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(given = "a datalake cluster", when = "ldap are attached", then = "cluster creation is failed by missing hive and ranger rds")
    public void testCreateDatalakeClusterWithoutRds(TestContext testContext) {
        String ldapName = resourcePropertyProvider().getName();
        String clusterDefinitionName = resourcePropertyProvider().getName();
        testContext
                .given(ClusterDefinitionTestDto.class).withName(clusterDefinitionName)
                .withTag(of(SHARED_SERVICE_TAG), of(true)).withClusterDefinition(VALID_DL_BP)
                .when(clusterDefinitionTestClient.createV4())
                .given(LdapTestDto.class).withName(ldapName)
                .when(ldapTestClient.createV4())
                .given(MASTER.name(), InstanceGroupTestDto.class).valid().withHostGroup(MASTER).withNodeCount(1)
                .given(ClusterTestDto.class)
                .withClusterDefinitionName(clusterDefinitionName)
                .withAmbari(testContext.given(AmbariTestDto.class))
                .withLdapConfigName(ldapName)
                .withCloudStorage(cloudStorage())
                .init(StackTestDto.class)
                .withInstanceGroups(MASTER.name())
                .when(stackTestClient.createV4(), key(BAD_REQUEST_KEY))
                .expect(BadRequestException.class, key(BAD_REQUEST_KEY).withExpectedMessage("1. For a Datalake cluster \\(since you have selected a datalake "
                        + "ready cluster definition\\) you should provide at least one Hive rds/database configuration to the Cluster request\n"
                        + " 2. For a Datalake cluster \\(since you have selected a datalake ready cluster definition\\) you should provide at least "
                        + "one Ranger rds/database configuration to the Cluster request"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(given = "a datalake cluster", when = "without ldap, ranger and hive rds",
            then = "cluster creation is failed by missing hive and ranger rds and ldap")
    public void testCreateDatalakeClusterWithoutRdsAndLdap(TestContext testContext) {
        String clusterDefinitionName = resourcePropertyProvider().getName();
        testContext
                .given(ClusterDefinitionTestDto.class).withName(clusterDefinitionName)
                .withTag(of(SHARED_SERVICE_TAG), of(true)).withClusterDefinition(VALID_DL_BP)
                .when(clusterDefinitionTestClient.createV4())
                .given(MASTER.name(), InstanceGroupTestDto.class).valid().withHostGroup(MASTER).withNodeCount(1)
                .given(ClusterTestDto.class)
                .withClusterDefinitionName(clusterDefinitionName)
                .withAmbari(testContext.given(AmbariTestDto.class))
                .withCloudStorage(cloudStorage())
                .init(StackTestDto.class)
                .withInstanceGroups(MASTER.name())
                .when(stackTestClient.createV4(), key(BAD_REQUEST_KEY))
                .expect(BadRequestException.class, key(BAD_REQUEST_KEY).withExpectedMessage("1. For a Datalake cluster \\(since you have selected a datalake "
                        + "ready cluster definition\\) you should provide at least one Hive rds/database configuration to the Cluster request\n"
                        + " 2. For a Datalake cluster \\(since you have selected a datalake ready cluster definition\\) you should provide at least one "
                        + "Ranger rds/database configuration to the Cluster request\n"
                        + " 3. For a Datalake cluster \\(since you have selected a datalake ready cluster definition\\) you should provide an LDAP "
                        + "configuration or its name/id to the Cluster request"))
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

    private Set<String> createSetOfNotNulls(String... elements) {
        Set<String> toReturn = new LinkedHashSet<>();
        for (String element : elements) {
            if (element != null) {
                toReturn.add(element);
            }
        }
        return toReturn;
    }

    private static StackTestDto checkClusterDefinitionTaggedWithSharedService(TestContext testContext, StackTestDto stack, CloudbreakClient cloudbreakClient) {
        Map<String, Object> clusterDefinitionTags = stack.getResponse().getCluster().getClusterDefinition().getTags();
        if (!clusterDefinitionTags.containsKey(SHARED_SERVICE_TAG) || clusterDefinitionTags.get(SHARED_SERVICE_TAG) == null
                || !(clusterDefinitionTags.get(SHARED_SERVICE_TAG) instanceof Boolean)
                || !((Boolean) clusterDefinitionTags.get(SHARED_SERVICE_TAG))) {
            throw new TestFailException("shared service tag has not passed properly (or at all) to the cluster definition");
        }
        return stack;
    }

    private List<AssertionV2<StackTestDto>> cloudStorageParametersHasPassedToAmbariClusterDefinition() {
        List<AssertionV2<StackTestDto>> verifications = new LinkedList<>();
        verifications.add(clusterDefinitionPostToAmbariContains(cloudStorage().getAdls().getAccountName()));
        verifications.add(clusterDefinitionPostToAmbariContains(cloudStorage().getAdls().getClientId()));
        verifications.add(clusterDefinitionPostToAmbariContains(cloudStorage().getAdls().getCredential()));
        return verifications;
    }

    private List<AssertionV2<StackTestDto>> ldapParametersHasPassedToAmbariClusterDefinition(String ldapName) {
        List<AssertionV2<StackTestDto>> verifications = new LinkedList<>();
        verifications.add(clusterDefinitionPostToAmbariContains(ldapRequest(ldapName).getUserDnPattern()));
        verifications.add(clusterDefinitionPostToAmbariContains(ldapRequest(ldapName).getBindPassword()));
        verifications.add(clusterDefinitionPostToAmbariContains(ldapRequest(ldapName).getBindDn()));
        verifications.add(clusterDefinitionPostToAmbariContains(ldapRequest(ldapName).getHost()));
        verifications.add(clusterDefinitionPostToAmbariContains(ldapRequest(ldapName).getUserSearchBase()));
        verifications.add(clusterDefinitionPostToAmbariContains(ldapRequest(ldapName).getGroupSearchBase()));
        verifications.add(clusterDefinitionPostToAmbariContains(ldapRequest(ldapName).getGroupNameAttribute()));
        verifications.add(clusterDefinitionPostToAmbariContains(ldapRequest(ldapName).getHost()));
        return verifications;
    }

    private List<AssertionV2<StackTestDto>> rdsParametersHasPassedToAmbariClusterDefinition(DatabaseV4Request hive, DatabaseV4Request ranger) {
        List<AssertionV2<StackTestDto>> verifications = new LinkedList<>();
        verifications.add(clusterDefinitionPostToAmbariContains("ranger_privelege_user_jdbc_url"));
        verifications.add(clusterDefinitionPostToAmbariContains(ranger.getConnectionURL()));
        verifications.add(clusterDefinitionPostToAmbariContains("javax.jdo.option.ConnectionURL"));
        verifications.add(clusterDefinitionPostToAmbariContains(hive.getConnectionURL()));
        verifications.add(clusterDefinitionPostToAmbariContains("org.apache.hadoop.fs.adl.AdlFileSystem"));
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

    private MockVerification clusterDefinitionPostToAmbariContains(String content) {
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