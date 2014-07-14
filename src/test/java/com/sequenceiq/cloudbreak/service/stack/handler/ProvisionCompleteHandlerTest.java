package com.sequenceiq.cloudbreak.service.stack.handler;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anySet;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupContext;

import reactor.event.Event;

public class ProvisionCompleteHandlerTest {
    @InjectMocks
    private ProvisionCompleteHandler underTest;

    @Mock
    private MetadataSetupContext metadataSetupContext;

    @Mock
    private RetryingStackUpdater retryingStackUpdater;

    private Event<ProvisionComplete> event;

    private Set<Resource> resourceSet;

    @Before
    public void setUp() {
        underTest = new ProvisionCompleteHandler();
        MockitoAnnotations.initMocks(this);
        resourceSet = new HashSet<>();
        resourceSet.add(new Resource(ResourceType.CLOUDFORMATION_TEMPLATE_NAME, "", new Stack()));
        event = createEvent();
    }

    @Test
    public void testAcceptProvisionCompleteEvent() {
        // GIVEN
        doNothing().when(metadataSetupContext).setupMetadata(any(CloudPlatform.class), anyLong());
        given(retryingStackUpdater.updateStackResources(any(Long.class), anySet())).willReturn(null);
        // WHEN
        underTest.accept(event);
        // THEN
        verify(metadataSetupContext, times(1)).setupMetadata(any(CloudPlatform.class), anyLong());
    }


    private Event<ProvisionComplete> createEvent() {
        return new Event<>(new ProvisionComplete(CloudPlatform.AWS, 1L, resourceSet));
    }
}
