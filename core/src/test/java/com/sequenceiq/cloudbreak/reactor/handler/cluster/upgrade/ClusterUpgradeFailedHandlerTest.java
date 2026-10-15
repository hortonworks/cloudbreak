package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade;

import static com.sequenceiq.cloudbreak.service.image.ImageTestUtil.getImage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
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
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailHandledRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailedRequest;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
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
    void failedUpgradeKeepsResolvedPropertiesAcrossSerialization() throws Exception {
        ReflectionTestUtils.setField(underTest, "syncAfterFailureEnabled", true);
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.3.2");
        ClusterUpgradeFailedRequest request = new ClusterUpgradeFailedRequest(STACK_ID, new RuntimeException("upgrade failed"),
                DetailedStackStatus.CLUSTER_UPGRADE_FAILED, properties);
        ClusterUpgradeFailedRequest restored = JsonUtil.readValue(JsonUtil.writeValueAsString(request), ClusterUpgradeFailedRequest.class);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);

        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(restored)));

        assertThat(result).isInstanceOf(ClusterUpgradeFailHandledRequest.class);
        assertThat(restored.getClusterUpgradeProperties()).isEqualTo(properties);
        verify(cmSyncerService).syncFromCmToDb(stack, Set.of(), restored.getClusterUpgradeProperties());
    }

    @Test
    void legacyFailedRequestWithoutPropertiesCanBeRestored() throws Exception {
        String json = JsonUtil.writeValueAsString(Map.of("@type", ClusterUpgradeFailedRequest.class.getName(),
                "resourceId", STACK_ID, "detailedStatus", "CLUSTER_UPGRADE_FAILED"));
        ClusterUpgradeFailedRequest request = JsonUtil.readValue(json, ClusterUpgradeFailedRequest.class);

        assertThat(request.getClusterUpgradeProperties()).isNull();
        assertThat(request.getResourceId()).isEqualTo(STACK_ID);
    }

    @Test
    public void testHandleClusterUpgradeFailedRequestWhenNoConclusionFound() {
        ReflectionTestUtils.setField(underTest, "syncAfterFailureEnabled", true);
        Set<Image> images = Set.of(getImage(true, "uuid1", "7.1.0", null), getImage(true, "uuid2", "7.2.1", null));
        when(stackService.getByIdWithListsInTransaction(eq(STACK_ID))).thenReturn(stack);
        when(cmSyncImageCollectorService.collectImages(stack, Collections.emptySet())).thenReturn(images);
        ClusterUpgradeFailedRequest request = new ClusterUpgradeFailedRequest(STACK_ID, new RuntimeException("error"),
                DetailedStackStatus.CLUSTER_MANAGER_UPGRADE_FAILED, null);
        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);

        assertThat(selectable).isInstanceOf(ClusterUpgradeFailHandledRequest.class);

        verify(stackService, times(1)).getByIdWithListsInTransaction(eq(STACK_ID));
    }

}