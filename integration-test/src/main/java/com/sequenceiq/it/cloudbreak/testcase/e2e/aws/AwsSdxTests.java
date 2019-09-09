package com.sequenceiq.it.cloudbreak.testcase.e2e.aws;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.postgresql.Driver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.EbsInstanceBlockDevice;
import com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.it.cloudbreak.client.DatabaseTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.aws.AwsProperties;
import com.sequenceiq.it.cloudbreak.config.DatabaseProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.database.DatabaseTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.BasicSdxTests;

public class AwsSdxTests extends BasicSdxTests {

    private static final String POSTGRES_DB = "postgres";

    private static final String CREATE_DATABASE_TEMPLATE = "create database %s;";

    private static final String DROP_DATABASE_TEMPLATE = "drop database %s;";

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private DatabaseTestClient databaseTestClient;

    @Inject
    private DatabaseProperties databaseProperties;

    @Inject
    private AwsProperties awsProperties;

    private JdbcTemplate jdbcTemplate;

    private String cldmgr;

    private String cldrmgrSrvRepMgr;

    private String hivedb;

    private String rangerdb;

    @Override
    protected void setupTest(TestContext testContext) {
        super.setupTest(testContext);

        createJdbcTemplate();
        setUpDatabaseNames();
        executeDatabaseDdl(CREATE_DATABASE_TEMPLATE);
    }

    private void executeDatabaseDdl(String databaseStatement) {
        jdbcTemplate.execute(String.format(databaseStatement, cldmgr));
        jdbcTemplate.execute(String.format(databaseStatement, cldrmgrSrvRepMgr));
        jdbcTemplate.execute(String.format(databaseStatement, hivedb));
        jdbcTemplate.execute(String.format(databaseStatement, rangerdb));
    }

    private void setUpDatabaseNames() {
        cldmgr = "cldmgrdb" + UUID.randomUUID().toString().replace("-", "");
        cldrmgrSrvRepMgr = "cldrmgrsrvrepmgrdb" + UUID.randomUUID().toString().replace("-", "");
        hivedb = "hivedb" + UUID.randomUUID().toString().replace("-", "");
        rangerdb = "rangerdb" + UUID.randomUUID().toString().replace("-", "");
    }

    private void createJdbcTemplate() {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(Driver.class);
        dataSource.setUrl(databaseProperties.getConnectionUrl() + POSTGRES_DB);
        dataSource.setUsername(databaseProperties.getUsername());
        dataSource.setPassword(databaseProperties.getPassword());

        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @AfterMethod
    public void tearDown() {
        executeDatabaseDdl(DROP_DATABASE_TEMPLATE);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "recovery called on the master host",
            then = "SDX recovery should be successful, the cluster should be available AND deletable"
    )
    public void testRecoverSDX(TestContext testContext) throws IOException {
        String cm = resourcePropertyProvider().getName();
        String cluster = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        String sdx = resourcePropertyProvider().getName();

        AmazonEC2Client amazonEC2Client = createAmazonEC2Client();
        createRdsConfigs(testContext);

        List<String> volumeIds = new ArrayList<>();
        testContext
                .given(cm, ClouderaManagerTestDto.class)
                .given(cluster, ClusterTestDto.class)
                .withValidateBlueprint(Boolean.FALSE)
                .withClouderaManager(cm)
                .withDatabases(Set.of(cldmgr, cldrmgrSrvRepMgr, rangerdb, hivedb))
                .given(stack, StackTestDto.class)
                .withName(sdx)
                .withDomainName("sdx")
                .withClusterNameAsSubdomain(Boolean.FALSE)
                .withHostgroupNameAsHostname(Boolean.TRUE)
                .withCluster(cluster)

                .given(sdx, SdxInternalTestDto.class)
                .withName(sdx)
                .withStackRequest(stack, cluster)
                .when(sdxTestClient().createInternal(), key(sdx))
                .await(SDX_RUNNING)

                .given(stack, StackTestDto.class)
                .when(stackTestClient.getV4())
                .then((tc, testDto, client) -> {
                    String instanceId = testDto.getInstanceId("master");
                    volumeIds.addAll(getVolumeIdList(amazonEC2Client, instanceId));
                    return testDto;
                })
                .when(stackTestClient.repairCluster())
                .await(STACK_AVAILABLE)
                .when(stackTestClient.getV4())
                .then((tc, testDto, client) -> {
                    String instanceId = testDto.getInstanceId("master");
                    List<String> volumeIdsAfterRepair = getVolumeIdList(amazonEC2Client, instanceId);
                    volumeIdsAfterRepair.sort(Comparator.naturalOrder());
                    volumeIds.sort(Comparator.naturalOrder());
                    assertEquals(volumeIdsAfterRepair, volumeIds, "Volumes should be reattached");
                    return testDto;
                })

                .given(sdx, SdxInternalTestDto.class)
                .then((tc, testDto, client) -> sdxTestClient().deleteInternal().action(tc, testDto, client))
                .await(SDX_DELETED)
                .validate();
    }

    private AmazonEC2Client createAmazonEC2Client() {
        String accessKey = awsProperties.getCredential().getAccessKeyId();
        String secretKey = awsProperties.getCredential().getSecretKey();
        AmazonEC2Client ec2Client = new AmazonEC2Client(new BasicAWSCredentials(accessKey, secretKey));
        ec2Client.setRegion(Region.getRegion(Regions.fromName(awsProperties.getRegion())));
        return ec2Client;
    }

    private List<String> getVolumeIdList(AmazonEC2Client amazonEC2Client, String instanceId) {
        DescribeInstancesResult master = amazonEC2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceId));
        return master.getReservations().get(0).getInstances().get(0).getBlockDeviceMappings().stream()
                .filter(dev -> !"/dev/xvda".equals(dev.getDeviceName()))
                .map(InstanceBlockDeviceMapping::getEbs)
                .map(EbsInstanceBlockDevice::getVolumeId)
                .collect(Collectors.toList());
    }

    private void createRdsConfigs(TestContext testContext) {
        testContext
                .given(cldmgr, DatabaseTestDto.class)
                .withName(cldmgr)
                .withType(DatabaseType.CLOUDERA_MANAGER.name())
                .withConnectionUserName(databaseProperties.getUsername())
                .withConnectionPassword(databaseProperties.getPassword())
                .withConnectionURL(databaseProperties.getConnectionUrl() + cldmgr)
                .when(databaseTestClient.createV4())

                .given(cldrmgrSrvRepMgr, DatabaseTestDto.class)
                .withName(cldrmgrSrvRepMgr)
                .withType(DatabaseType.CLOUDERA_MANAGER_MANAGEMENT_SERVICE_REPORTS_MANAGER.name())
                .withConnectionUserName(databaseProperties.getUsername())
                .withConnectionPassword(databaseProperties.getPassword())
                .withConnectionURL(databaseProperties.getConnectionUrl() + cldrmgrSrvRepMgr)
                .when(databaseTestClient.createV4())

                .given(rangerdb, DatabaseTestDto.class)
                .withName(rangerdb)
                .withType(DatabaseType.RANGER.name())
                .withConnectionUserName(databaseProperties.getUsername())
                .withConnectionPassword(databaseProperties.getPassword())
                .withConnectionURL(databaseProperties.getConnectionUrl() + rangerdb)
                .when(databaseTestClient.createV4())

                .given(hivedb, DatabaseTestDto.class)
                .withName(hivedb)
                .withType(DatabaseType.HIVE.name())
                .withConnectionUserName(databaseProperties.getUsername())
                .withConnectionPassword(databaseProperties.getPassword())
                .withConnectionURL(databaseProperties.getConnectionUrl() + hivedb)
                .when(databaseTestClient.createV4());
    }
}
