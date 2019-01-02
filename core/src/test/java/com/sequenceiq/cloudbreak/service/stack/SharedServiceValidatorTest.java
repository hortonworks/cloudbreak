package com.sequenceiq.cloudbreak.service.stack;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;

@RunWith(MockitoJUnitRunner.class)
public class SharedServiceValidatorTest {

    private static final String DATALAKE_NAME = "datalake";

    private static final String LDAP_NAME = "ldap";

    private static final String RANGER_DB_NAME = "ranger";

    private static final String HIVE_DB_NAME = "hivetest";

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private StackViewService stackViewService;

    @InjectMocks
    private SharedServiceValidator underTest;

    @Test
    public void testWithValidRequest() {
        ClusterV4Request clusterRequest = new ClusterV4Request();
        clusterRequest.setDatabases(Sets.newHashSet(RANGER_DB_NAME, HIVE_DB_NAME));
        clusterRequest.setLdapName(LDAP_NAME);
        clusterRequest.setSharedService(new SharedServiceV4Request());
        clusterRequest.getSharedService().setSharedClusterName(DATALAKE_NAME);
        StackV4Request stackRequest = new StackV4Request();
        stackRequest.setCluster(clusterRequest);
        stackRequest.setCloudPlatform(CloudPlatform.GCP);
        Workspace workspace = new Workspace();
        StackView stackView = new StackView();
        stackView.setCloudPlatform("GCP");
        when(stackViewService.findByName(eq(DATALAKE_NAME), anyLong())).thenReturn(stackView);
        RDSConfig ranger = new RDSConfig();
        ranger.setType("RANGER");
        when(rdsConfigService.getByNameForWorkspace(eq(RANGER_DB_NAME), any())).thenReturn(ranger);
        RDSConfig hive = new RDSConfig();
        hive.setType("HIVE");
        when(rdsConfigService.getByNameForWorkspace(eq(HIVE_DB_NAME), any())).thenReturn(hive);

        ValidationResult validationResult = underTest.checkSharedServiceStackRequirements(stackRequest, workspace);

        assertFalse(validationResult.hasError());
    }

    @Test
    public void testWithMissingHive() {
        ClusterV4Request clusterRequest = new ClusterV4Request();
        clusterRequest.setDatabases(Sets.newHashSet(RANGER_DB_NAME, HIVE_DB_NAME));
        clusterRequest.setLdapName(LDAP_NAME);
        clusterRequest.setSharedService(new SharedServiceV4Request());
        clusterRequest.getSharedService().setSharedClusterName(DATALAKE_NAME);
        StackV4Request stackRequest = new StackV4Request();
        stackRequest.setCluster(clusterRequest);
        stackRequest.setCloudPlatform(CloudPlatform.GCP);
        Workspace workspace = new Workspace();
        StackView stackView = new StackView();
        stackView.setCloudPlatform("GCP");
        when(stackViewService.findByName(eq(DATALAKE_NAME), anyLong())).thenReturn(stackView);
        RDSConfig ranger = new RDSConfig();
        ranger.setType("RANGER");
        when(rdsConfigService.getByNameForWorkspace(eq(RANGER_DB_NAME), any())).thenReturn(ranger);
        when(rdsConfigService.getByNameForWorkspace(eq(HIVE_DB_NAME), any())).thenReturn(null);

        ValidationResult validationResult = underTest.checkSharedServiceStackRequirements(stackRequest, workspace);

        assertTrue(validationResult.hasError());
        assertEquals(1L, validationResult.getErrors().size());
        assertThat(validationResult.getErrors().get(0), containsString("HIVE"));
    }

    @Test
    public void testWithMissingRangerAndWrongCloudPlatform() {
        ClusterV4Request clusterRequest = new ClusterV4Request();
        clusterRequest.setDatabases(Sets.newHashSet(RANGER_DB_NAME, HIVE_DB_NAME));
        clusterRequest.setLdapName(LDAP_NAME);
        clusterRequest.setSharedService(new SharedServiceV4Request());
        clusterRequest.getSharedService().setSharedClusterName(DATALAKE_NAME);
        StackV4Request stackRequest = new StackV4Request();
        stackRequest.setCluster(clusterRequest);
        stackRequest.setCloudPlatform(CloudPlatform.AWS);
        Workspace workspace = new Workspace();
        StackView stackView = new StackView();
        stackView.setCloudPlatform("GCP");
        when(stackViewService.findByName(eq(DATALAKE_NAME), anyLong())).thenReturn(stackView);
        RDSConfig hive = new RDSConfig();
        hive.setType("HIVE");
        when(rdsConfigService.getByNameForWorkspace(eq(HIVE_DB_NAME), any())).thenReturn(hive);
        when(rdsConfigService.getByNameForWorkspace(eq(RANGER_DB_NAME), any())).thenReturn(null);

        ValidationResult validationResult = underTest.checkSharedServiceStackRequirements(stackRequest, workspace);

        assertTrue(validationResult.hasError());
        assertEquals(2L, validationResult.getErrors().size());
        assertThat(validationResult.getErrors().get(0), containsString("cloud platform"));
        assertThat(validationResult.getErrors().get(1), containsString("RANGER"));
    }

    @Test
    public void testWithMissingLdap() {
        ClusterV4Request clusterRequest = new ClusterV4Request();
        clusterRequest.setDatabases(Sets.newHashSet(RANGER_DB_NAME, HIVE_DB_NAME));
        clusterRequest.setSharedService(new SharedServiceV4Request());
        clusterRequest.getSharedService().setSharedClusterName(DATALAKE_NAME);
        StackV4Request stackRequest = new StackV4Request();
        stackRequest.setCluster(clusterRequest);
        stackRequest.setCloudPlatform(CloudPlatform.GCP);
        Workspace workspace = new Workspace();
        StackView stackView = new StackView();
        stackView.setCloudPlatform("GCP");
        when(stackViewService.findByName(eq(DATALAKE_NAME), anyLong())).thenReturn(stackView);
        RDSConfig ranger = new RDSConfig();
        ranger.setType("RANGER");
        when(rdsConfigService.getByNameForWorkspace(eq(RANGER_DB_NAME), any())).thenReturn(ranger);
        RDSConfig hive = new RDSConfig();
        hive.setType("HIVE");
        when(rdsConfigService.getByNameForWorkspace(eq(HIVE_DB_NAME), any())).thenReturn(hive);

        ValidationResult validationResult = underTest.checkSharedServiceStackRequirements(stackRequest, workspace);

        assertTrue(validationResult.hasError());
        assertEquals(1L, validationResult.getErrors().size());
        assertThat(validationResult.getErrors().get(0), containsString("LDAP"));
    }
}