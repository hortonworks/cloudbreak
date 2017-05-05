package com.sequenceiq.cloudbreak.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.model.RDSBuildRequest;
import com.sequenceiq.cloudbreak.api.model.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.RDSDatabase;
import com.sequenceiq.cloudbreak.api.model.RdsBuildResult;
import com.sequenceiq.cloudbreak.common.type.RdsType;
import com.sequenceiq.cloudbreak.controller.validation.ldapconfig.LdapConfigValidator;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionBuilder;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionValidator;
import com.sequenceiq.cloudbreak.repository.RdsConfigRepository;

@RunWith(MockitoJUnitRunner.class)
public class UtilControllerTest {

    @InjectMocks
    private UtilController underTest;

    @Mock
    private RdsConnectionValidator rdsConnectionValidator;

    @Mock
    private RdsConnectionBuilder rdsConnectionBuilder;

    @Mock
    private LdapConfigValidator ldapConfigValidator;

    @Mock
    private RdsConfigRepository rdsConfigRepository;

    @Before
    public void before() {
        doNothing().when(rdsConnectionBuilder).buildRdsConnection(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void databaseCreationTestWhenClusterContainsHyphens() {
        RDSBuildRequest rdsBuildRequest = rdsBuildRequest(rdsConfigRequest(), "testcluster-123");

        RdsBuildResult rdsBuildResult = underTest.buildRdsConnection(rdsBuildRequest);

        assertEquals("testcluster123ambari", rdsBuildResult.getAmbariDbName());
        assertEquals("testcluster123hive", rdsBuildResult.getHiveDbName());
        assertEquals("testcluster123ranger", rdsBuildResult.getRangerDbName());
    }

    @Test
    public void databaseCreationTestWhenClusterContainsSpecialCharachters() {
        RDSBuildRequest rdsBuildRequest = rdsBuildRequest(rdsConfigRequest(), "testcluster-123??#@234");

        RdsBuildResult rdsBuildResult = underTest.buildRdsConnection(rdsBuildRequest);

        assertEquals("testcluster123234ambari", rdsBuildResult.getAmbariDbName());
        assertEquals("testcluster123234hive", rdsBuildResult.getHiveDbName());
        assertEquals("testcluster123234ranger", rdsBuildResult.getRangerDbName());
    }

    private RDSBuildRequest rdsBuildRequest(RDSConfigRequest rdsConfigRequest, String clusterName) {
        RDSBuildRequest rdsBuildRequest = new RDSBuildRequest();

        rdsBuildRequest.setRdsConfigRequest(rdsConfigRequest);
        rdsBuildRequest.setClusterName(clusterName);

        return rdsBuildRequest;
    }

    private RDSConfigRequest rdsConfigRequest() {
        RDSConfigRequest rdsConfigRequest = new RDSConfigRequest();

        rdsConfigRequest.setConnectionPassword("testPassword");
        rdsConfigRequest.setConnectionUserName("testUserName");
        rdsConfigRequest.setName("testName");
        rdsConfigRequest.setValidated(false);
        rdsConfigRequest.setHdpVersion("2.6");
        rdsConfigRequest.setType(RdsType.HIVE);
        rdsConfigRequest.setConnectionURL("jdbc:postgres://testdb:5432/tesdb");
        rdsConfigRequest.setDatabaseType(RDSDatabase.POSTGRES);

        return rdsConfigRequest;
    }
}