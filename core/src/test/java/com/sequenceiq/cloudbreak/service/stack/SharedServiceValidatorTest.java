package com.sequenceiq.cloudbreak.service.stack;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;

@RunWith(MockitoJUnitRunner.class)
public class SharedServiceValidatorTest {

    private static final String DATALAKE_NAME = "datalake";

    private static final String LDAP_NAME = "ldap";

    private static final String RANGER_DB_NAME = "ranger";

    private static final String HIVE_DB_NAME = "hivetest";

    private static final String RANGER_TYPE_STRING = "RANGER";

    private static final String HIVE_TYPE_STRING = "HIVE";

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private StackViewService stackViewService;

    @Mock
    private BlueprintService blueprintService;

    @InjectMocks
    private SharedServiceValidator underTest;

    @Test
    public void testWithValidRequest() {
        StackV4Request stackRequest = getStackV4Request(CloudPlatform.GCP, LDAP_NAME);
        when(stackViewService.findByName(eq(DATALAKE_NAME), anyLong())).thenReturn(Optional.ofNullable(getStackView()));
        when(rdsConfigService.getByNameForWorkspace(eq(RANGER_DB_NAME), any())).thenReturn(getDatabase(RANGER_TYPE_STRING));
        when(rdsConfigService.getByNameForWorkspace(eq(HIVE_DB_NAME), any())).thenReturn(getDatabase(HIVE_TYPE_STRING));
        when(blueprintService.getByNameForWorkspaceId(anyString(), anyLong())).thenReturn(mock(Blueprint.class));
        when(blueprintService.isAmbariBlueprint(any())).thenReturn(true);

        ValidationResult validationResult = underTest.checkSharedServiceStackRequirements(stackRequest, getWorkspace());

        assertFalse(validationResult.hasError());
    }

    @Test
    public void testWithMissingHive() {
        StackV4Request stackRequest = getStackV4Request(CloudPlatform.GCP, LDAP_NAME);
        when(stackViewService.findByName(eq(DATALAKE_NAME), anyLong())).thenReturn(Optional.ofNullable(getStackView()));
        when(rdsConfigService.getByNameForWorkspace(eq(RANGER_DB_NAME), any())).thenReturn(getDatabase(RANGER_TYPE_STRING));
        when(rdsConfigService.getByNameForWorkspace(eq(HIVE_DB_NAME), any())).thenReturn(null);
        when(blueprintService.getByNameForWorkspaceId(anyString(), anyLong())).thenReturn(mock(Blueprint.class));
        when(blueprintService.isAmbariBlueprint(any())).thenReturn(true);

        ValidationResult validationResult = underTest.checkSharedServiceStackRequirements(stackRequest, getWorkspace());

        assertTrue(validationResult.hasError());
        assertEquals(1L, validationResult.getErrors().size());
        assertThat(validationResult.getErrors().get(0), containsString(HIVE_TYPE_STRING));
    }

    @Test
    public void testWithMissingRangerAndWrongCloudPlatform() {
        StackV4Request stackRequest = getStackV4Request(CloudPlatform.AWS, LDAP_NAME);
        when(stackViewService.findByName(eq(DATALAKE_NAME), anyLong())).thenReturn(Optional.ofNullable(getStackView()));
        when(rdsConfigService.getByNameForWorkspace(eq(HIVE_DB_NAME), any())).thenReturn(getDatabase(HIVE_TYPE_STRING));
        when(rdsConfigService.getByNameForWorkspace(eq(RANGER_DB_NAME), any())).thenReturn(null);
        when(blueprintService.getByNameForWorkspaceId(anyString(), anyLong())).thenReturn(mock(Blueprint.class));
        when(blueprintService.isAmbariBlueprint(any())).thenReturn(true);

        ValidationResult validationResult = underTest.checkSharedServiceStackRequirements(stackRequest, getWorkspace());

        assertTrue(validationResult.hasError());
        assertEquals(2L, validationResult.getErrors().size());
        assertThat(validationResult.getErrors().get(0), containsString("cloud platform"));
        assertThat(validationResult.getErrors().get(1), containsString(RANGER_TYPE_STRING));
    }

    @Test
    public void testWithMissingLdap() {
        StackV4Request stackRequest = getStackV4Request(CloudPlatform.GCP, null);
        when(stackViewService.findByName(eq(DATALAKE_NAME), anyLong())).thenReturn(Optional.ofNullable(getStackView()));
        when(rdsConfigService.getByNameForWorkspace(eq(RANGER_DB_NAME), any())).thenReturn(getDatabase(RANGER_TYPE_STRING));
        when(rdsConfigService.getByNameForWorkspace(eq(HIVE_DB_NAME), any())).thenReturn(getDatabase(HIVE_TYPE_STRING));
        when(blueprintService.getByNameForWorkspaceId(anyString(), anyLong())).thenReturn(mock(Blueprint.class));
        when(blueprintService.isAmbariBlueprint(any())).thenReturn(true);

        ValidationResult validationResult = underTest.checkSharedServiceStackRequirements(stackRequest, getWorkspace());

        assertTrue(validationResult.hasError());
        assertEquals(1L, validationResult.getErrors().size());
        assertThat(validationResult.getErrors().get(0), containsString("LDAP"));
    }

    private RDSConfig getDatabase(String type) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setType(type);
        return rdsConfig;
    }

    private StackV4Request getStackV4Request(CloudPlatform cloudPlatform, String ldapName) {
        ClusterV4Request clusterRequest = new ClusterV4Request();
        clusterRequest.setDatabases(Sets.newHashSet(RANGER_DB_NAME, HIVE_DB_NAME));
        clusterRequest.setLdapName(ldapName);
        AmbariV4Request ambariRequest = new AmbariV4Request();
        clusterRequest.setBlueprintName("test-blueprint");
        clusterRequest.setAmbari(ambariRequest);
        StackV4Request stackRequest = new StackV4Request();
        stackRequest.setSharedService(new SharedServiceV4Request());
        stackRequest.getSharedService().setDatalakeName(DATALAKE_NAME);
        stackRequest.setCluster(clusterRequest);
        stackRequest.setCloudPlatform(cloudPlatform);
        return stackRequest;
    }

    private StackView getStackView() {
        StackView stackView = new StackView();
        stackView.setCloudPlatform("GCP");
        stackView.setWorkspace(getWorkspace());
        return stackView;
    }

    private Workspace getWorkspace() {
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        return workspace;
    }

}