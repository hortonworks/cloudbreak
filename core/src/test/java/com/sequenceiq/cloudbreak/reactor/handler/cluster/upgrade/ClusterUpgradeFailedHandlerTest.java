package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade;

import static com.sequenceiq.cloudbreak.service.image.ImageTestUtil.getImage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailHandledRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailedRequest;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.CmSyncerService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradeFailedHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private CmSyncerService cmSyncerService;

    @Mock
    private StackService stackService;

    @Mock
    private ConclusionCheckerService conclusionCheckerService;

    @InjectMocks
    private ClusterUpgradeFailedHandler underTest;

    @Test
    public void testHandleClusterUpgradeFailedRequestWhenNoConclusionFound() {
        ReflectionTestUtils.setField(underTest, "syncAfterFailureEnabled", true);
        when(stackService.getByIdWithListsInTransaction(eq(STACK_ID))).thenReturn(TestUtil.stack());
        Set<Image> images = Set.of(getImage(true, "uuid1", "7.1.0"), getImage(true, "uuid2", "7.2.1"));
        ClusterUpgradeFailedRequest request = new ClusterUpgradeFailedRequest(STACK_ID, new RuntimeException("error"),
                DetailedStackStatus.CLUSTER_MANAGER_UPGRADE_FAILED, images);
        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);

        assertThat(selectable).isInstanceOf(ClusterUpgradeFailHandledRequest.class);

        verify(stackService, times(1)).getByIdWithListsInTransaction(eq(STACK_ID));
    }

}