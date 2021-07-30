package com.sequenceiq.cloudbreak.service.upgrade.sync.db;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterComponentUpdater;
import com.sequenceiq.cloudbreak.service.upgrade.StackComponentUpdater;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationResult;

@ExtendWith(MockitoExtension.class)
public class ComponentPersistingServiceTest {

    @Mock
    private CmSyncResultMergerService cmSyncResultMergerService;

    @Mock
    private StackComponentUpdater stackComponentUpdater;

    @Mock
    private ClusterComponentUpdater clusterComponentUpdater;

    @InjectMocks
    private ComponentPersistingService underTest;

    @Test
    void testPersistComponentsToDb() {
        Stack stack = new Stack();
        CmSyncOperationResult cmSyncOperationResult = new CmSyncOperationResult(null, null);
        Set<Component> foundComponents = Set.of(new Component());
        when(cmSyncResultMergerService.merge(stack, cmSyncOperationResult)).thenReturn(foundComponents);

        underTest.persistComponentsToDb(stack, cmSyncOperationResult);

        verify(stackComponentUpdater).updateComponentsByStackId(stack, foundComponents, false);
        verify(clusterComponentUpdater).updateClusterComponentsByStackId(stack, foundComponents, false);
    }
}
