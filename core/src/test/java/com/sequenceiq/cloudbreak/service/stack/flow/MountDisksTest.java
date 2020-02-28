package com.sequenceiq.cloudbreak.service.stack.flow;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@RunWith(MockitoJUnitRunner.class)
public class MountDisksTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private StackService stackService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private ResourceAttributeUtil resourceAttributeUtil;

    @Mock
    private ResourceService resourceService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private HostOrchestratorResolver hostOrchestratorResolver;

    @Mock
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @InjectMocks
    private MountDisks underTest;

    @Mock
    private Stack stack;

    @Test
    public void mountDisksOnNewNodesShouldUseReachableNodes() throws CloudbreakException {
        Set<String> newNodeAddresses = Set.of("1.1.1.1");
        try {
            when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
            when(stack.getPlatformVariant()).thenReturn(CloudConstants.MOCK);

            expectedException.expect(NullPointerException.class);
            underTest.mountDisksOnNewNodes(1L, newNodeAddresses);
        } catch (Exception e) {
            verify(stackUtil).collectReachableNodes(stack);
            verify(stackUtil).collectNewNodesWithDiskData(stack, newNodeAddresses);
            throw e;
        }
    }

}