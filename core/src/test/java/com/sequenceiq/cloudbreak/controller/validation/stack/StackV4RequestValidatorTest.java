package com.sequenceiq.cloudbreak.controller.validation.stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.controller.validation.template.InstanceTemplateV4RequestValidator;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.ldap.LdapConfigService;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.State;

@RunWith(MockitoJUnitRunner.class)
public class StackV4RequestValidatorTest extends StackRequestValidatorTestBase {

    private static final Long WORKSPACE_ID = 1L;

    private static final String TEST_HIVE_RDS_NAME = "hive-rds";

    private static final String TEST_RANGER_RDS_NAME = "ranger-rds";

    private static final String TEST_LDAP_NAME = "ldap";

    private static final String TEST_BP_NAME = "testBpName";

    private static final String CREDENTIAL_NAME = "someCred";

    private static final String ENVIRONMENT_NAME = "someEnvironment";

    private static final String RDS_ERROR_MESSAGE_FORMAT = "For a Datalake cluster (since you have selected a datalake ready blueprint) "
            + "you should provide at least one %s rds/database configuration to the Cluster request";

    private static final String LACK_OF_LDAP_MESSAGE = "For a Datalake cluster (since you have selected a datalake ready blueprint) you should provide"
            + " an LDAP configuration or its name/id to the Cluster request";

    @Spy
    private final InstanceTemplateV4RequestValidator templateRequestValidator = new InstanceTemplateV4RequestValidator();

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @InjectMocks
    private StackV4RequestValidator underTest;

    @Mock
    private Blueprint blueprint;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private EnvironmentClientService environmentClientService;

    @Mock
    private Credential credential;

    @Mock
    private CloudRegions cloudRegions;

    @Mock
    private LdapConfigService ldapConfigService;

    public StackV4RequestValidatorTest() {
        super(LoggerFactory.getLogger(StackV4RequestValidatorTest.class));
    }

    @Test
    public void testWithZeroRootVolumeSize() {
        assertNotNull(templateRequestValidator);
        StackV4Request stackRequest = stackRequestWithRootVolumeSize(0);
        ValidationResult validationResult = underTest.validate(stackRequest);
        assertEquals(State.ERROR, validationResult.getState());
    }

    @Test
    public void testWithNegativeRootVolumeSize() {
        StackV4Request stackRequest = stackRequestWithRootVolumeSize(-1);
        ValidationResult validationResult = underTest.validate(stackRequest);
        assertEquals(State.ERROR, validationResult.getState());
    }

    @Test
    public void testNullValueIsAllowedForRootVolumeSize() {
        StackV4Request stackRequest = stackRequestWithRootVolumeSize(null);
        ValidationResult validationResult = underTest.validate(stackRequest);
        assertEquals(State.VALID, validationResult.getState());
    }

    @Test
    public void testWithPositiveRootVolumeSize() {
        StackV4Request stackRequest = stackRequestWithRootVolumeSize(1);
        ValidationResult validationResult = underTest.validate(stackRequest);
        assertEquals(State.VALID, validationResult.getState());
    }

    private StackV4Request stackRequest() {
        InstanceTemplateV4Request templateRequest = new InstanceTemplateV4Request();
        InstanceGroupV4Request instanceGroupRequest = getInstanceGroupV4Request(templateRequest);
        ClusterV4Request clusterRequest = getCluster();
        return getStackV4Request(Collections.singletonList(instanceGroupRequest), clusterRequest);
    }

    private StackV4Request stackRequestWithRootVolumeSize(Integer rootVolumeSize) {
        InstanceTemplateV4Request templateRequest = new InstanceTemplateV4Request();
        templateRequest.setRootVolume(getRootVolume(rootVolumeSize));
        InstanceGroupV4Request instanceGroupRequest = getInstanceGroupV4Request(templateRequest);
        ClusterV4Request clusterRequest = getCluster();
        return getStackV4Request(Collections.singletonList(instanceGroupRequest), clusterRequest);
    }

    private VolumeV4Request getRootVolume(Integer size) {
        VolumeV4Request root = new VolumeV4Request();
        root.setSize(size);
        return root;
    }

    private InstanceGroupV4Request getInstanceGroupV4Request(InstanceTemplateV4Request templateRequest) {
        InstanceGroupV4Request instanceGroupRequest = new InstanceGroupV4Request();
        instanceGroupRequest.setName("master");
        instanceGroupRequest.setTemplate(templateRequest);
        return instanceGroupRequest;
    }

    private ClusterV4Request getCluster() {
        ClusterV4Request clusterRequest = new ClusterV4Request();
        clusterRequest.setBlueprintName(TEST_BP_NAME);
        return clusterRequest;
    }

    private StackV4Request getStackV4Request(List<InstanceGroupV4Request> instanceGroupRequests, ClusterV4Request clusterRequest) {
        StackV4Request stackRequest = new StackV4Request();
        stackRequest.setCluster(clusterRequest);
        stackRequest.setInstanceGroups(instanceGroupRequests);
        stackRequest.setEnvironmentCrn("envCrn");
        stackRequest.setPlacement(new PlacementSettingsV4Request());
        return stackRequest;
    }

    private RDSConfig rdsConfig(DatabaseType type) {
        RDSConfig rds = new RDSConfig();
        rds.setType(type.name());
        return rds;
    }

}