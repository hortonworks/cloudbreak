package com.sequenceiq.cloudbreak.service.decorator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.FileReaderUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateValidator;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintValidatorFactory;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.EmbeddedDatabaseService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.sharedservice.SharedServiceConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
class ClusterDecoratorTest {
    private static final String ACCOUNT_ID = "cloudera";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:test@cloudera.com";

    @InjectMocks
    private ClusterDecorator underTest;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private BlueprintValidatorFactory blueprintValidatorFactory;

    @Mock
    private CmTemplateValidator cmTemplateValidator;

    @Mock
    private StackService stackService;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private SharedServiceConfigProvider sharedServiceConfigProvider;

    @Mock
    private Stack stack;

    @Mock
    private User user;

    @Mock
    private EmbeddedDatabaseService embeddedDatabaseService;

    @BeforeEach
    void setUp() {
        when(blueprintValidatorFactory.createBlueprintValidator(any())).thenReturn(cmTemplateValidator);
            }

    @Test
    void testDecorateIfMethodCalledThenSharedServiceConfigProviderShouldBeCalledOnceToConfigureTheCluster() {
        Cluster expectedClusterInstance = new Cluster();
        Blueprint blueprint = getBlueprint();
        when(sharedServiceConfigProvider.configureCluster(any(Cluster.class)))
                .thenReturn(expectedClusterInstance);
        when(embeddedDatabaseService.isEmbeddedDatabaseOnAttachedDiskEnabled(stack, expectedClusterInstance)).thenReturn(false);
        Cluster result = underTest.decorate(expectedClusterInstance, createClusterV4Request(), blueprint, user, new Workspace(), stack);

        assertEquals(expectedClusterInstance, result);
        verify(sharedServiceConfigProvider, times(1)).configureCluster(any(Cluster.class));
    }

    private Blueprint getBlueprint() {
        Blueprint blueprint = new Blueprint();
        String blueprintText = FileReaderUtil.readResourceFile(this, "ha-components.bp");
        blueprint.setBlueprintText(blueprintText);
        return blueprint;
    }

    private ClusterV4Request createClusterV4Request() {
        return new ClusterV4Request();
    }
}
