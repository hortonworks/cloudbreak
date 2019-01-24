package com.sequenceiq.cloudbreak.service.stack;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
import com.sequenceiq.cloudbreak.api.model.stack.StackRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;

@RunWith(MockitoJUnitRunner.class)
public class SharedServiceValidatorTest {

    private static final String CLOUD_PLATFORM_1 = "GCP";

    private static final String CLOUD_PLATFORM_2 = "AWS";

    private static final long DATALAKE_ID = 1L;

    private static final long LDAP_CONFIG_ID = 2L;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private StackViewService stackViewService;

    @InjectMocks
    private SharedServiceValidator underTest;

    @Test
    public void testWithValidRequest() {
        RDSConfigRequest rangerRequest = new RDSConfigRequest();
        rangerRequest.setType(RdsType.RANGER.name());
        RDSConfigRequest hiveRequest = new RDSConfigRequest();
        hiveRequest.setType(RdsType.HIVE.name());
        ClusterRequest clusterRequest = new ClusterRequest();
        clusterRequest.setRdsConfigJsons(Set.of(rangerRequest, hiveRequest));
        clusterRequest.setLdapConfigId(LDAP_CONFIG_ID);
        StackRequest stackRequest = new StackRequest();
        stackRequest.setClusterRequest(clusterRequest);
        stackRequest.setClusterToAttach(DATALAKE_ID);
        stackRequest.setCloudPlatform(CLOUD_PLATFORM_1);
        Workspace workspace = new Workspace();
        StackView stackView = new StackView();
        stackView.setCloudPlatform(CLOUD_PLATFORM_1);
        when(stackViewService.findById(DATALAKE_ID)).thenReturn(Optional.of(stackView));

        ValidationResult validationResult = underTest.checkSharedServiceStackRequirements(stackRequest, workspace);

        assertFalse(validationResult.hasError());
    }

    @Test
    public void testWithMissingHive() {
        RDSConfigRequest rangerRequest = new RDSConfigRequest();
        rangerRequest.setType(RdsType.RANGER.name());
        ClusterRequest clusterRequest = new ClusterRequest();
        clusterRequest.setRdsConfigJsons(Set.of(rangerRequest));
        clusterRequest.setLdapConfigId(LDAP_CONFIG_ID);
        StackRequest stackRequest = new StackRequest();
        stackRequest.setClusterRequest(clusterRequest);
        stackRequest.setClusterToAttach(DATALAKE_ID);
        stackRequest.setCloudPlatform(CLOUD_PLATFORM_1);
        Workspace workspace = new Workspace();
        StackView stackView = new StackView();
        stackView.setCloudPlatform(CLOUD_PLATFORM_1);
        when(stackViewService.findById(DATALAKE_ID)).thenReturn(Optional.of(stackView));

        ValidationResult validationResult = underTest.checkSharedServiceStackRequirements(stackRequest, workspace);

        assertTrue(validationResult.hasError());
        assertEquals(1L, validationResult.getErrors().size());
        assertThat(validationResult.getErrors().get(0), containsString("HIVE"));
    }

    @Test
    public void testWithMissingRangerAndWrongCloudPlatform() {
        RDSConfigRequest hiveRequest = new RDSConfigRequest();
        hiveRequest.setType(RdsType.HIVE.name());
        ClusterRequest clusterRequest = new ClusterRequest();
        clusterRequest.setRdsConfigJsons(Set.of(hiveRequest));
        clusterRequest.setLdapConfigId(LDAP_CONFIG_ID);
        StackRequest stackRequest = new StackRequest();
        stackRequest.setClusterRequest(clusterRequest);
        stackRequest.setClusterToAttach(DATALAKE_ID);
        stackRequest.setCloudPlatform(CLOUD_PLATFORM_2);
        Workspace workspace = new Workspace();
        StackView stackView = new StackView();
        stackView.setCloudPlatform(CLOUD_PLATFORM_1);
        when(stackViewService.findById(DATALAKE_ID)).thenReturn(Optional.of(stackView));

        ValidationResult validationResult = underTest.checkSharedServiceStackRequirements(stackRequest, workspace);

        assertTrue(validationResult.hasError());
        assertEquals(2L, validationResult.getErrors().size());
        assertThat(validationResult.getErrors().get(0), containsString("cloud platform"));
        assertThat(validationResult.getErrors().get(1), containsString("RANGER"));
    }

    @Test
    public void testWithMissingLdap() {
        RDSConfigRequest rangerRequest = new RDSConfigRequest();
        rangerRequest.setType(RdsType.RANGER.name());
        RDSConfigRequest hiveRequest = new RDSConfigRequest();
        hiveRequest.setType(RdsType.HIVE.name());
        ClusterRequest clusterRequest = new ClusterRequest();
        clusterRequest.setRdsConfigJsons(Set.of(rangerRequest, hiveRequest));
        StackRequest stackRequest = new StackRequest();
        stackRequest.setClusterRequest(clusterRequest);
        stackRequest.setClusterToAttach(DATALAKE_ID);
        stackRequest.setCloudPlatform(CLOUD_PLATFORM_1);
        Workspace workspace = new Workspace();
        StackView stackView = new StackView();
        stackView.setCloudPlatform(CLOUD_PLATFORM_1);
        when(stackViewService.findById(DATALAKE_ID)).thenReturn(Optional.of(stackView));

        ValidationResult validationResult = underTest.checkSharedServiceStackRequirements(stackRequest, workspace);

        assertTrue(validationResult.hasError());
        assertEquals(1L, validationResult.getErrors().size());
        assertThat(validationResult.getErrors().get(0), containsString("LDAP"));
    }
}