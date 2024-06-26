package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade;

import static com.sequenceiq.cloudbreak.service.image.ImageTestUtil.getImage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailHandledRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailedRequest;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.CmSyncImageCollectorService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.CmSyncerService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradeFailedHandlerTest {

    private static final Long STACK_ID = 1L;

    private static final String USER_CRN = "user-crn";

    @Mock
    private CmSyncerService cmSyncerService;

    @Mock
    private StackService stackService;

    @Mock
    private ConclusionCheckerService conclusionCheckerService;

    @Mock
    private CmSyncImageCollectorService cmSyncImageCollectorService;

    @Mock
    private Stack stack;

    @InjectMocks
    private ClusterUpgradeFailedHandler underTest;

    @Test
    public void testHandleClusterUpgradeFailedRequestWhenNoConclusionFound() {
        ReflectionTestUtils.setField(underTest, "syncAfterFailureEnabled", true);
        Set<Image> images = Set.of(getImage(true, "uuid1", "7.1.0", null), getImage(true, "uuid2", "7.2.1", null));
        when(stackService.getByIdWithListsInTransaction(eq(STACK_ID))).thenReturn(stack);
        when(cmSyncImageCollectorService.collectImages(stack, Collections.emptySet())).thenReturn(images);
        ClusterUpgradeFailedRequest request = new ClusterUpgradeFailedRequest(STACK_ID, new RuntimeException("error"),
                DetailedStackStatus.CLUSTER_MANAGER_UPGRADE_FAILED);
        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);

        assertThat(selectable).isInstanceOf(ClusterUpgradeFailHandledRequest.class);

        verify(stackService, times(1)).getByIdWithListsInTransaction(eq(STACK_ID));
    }

}