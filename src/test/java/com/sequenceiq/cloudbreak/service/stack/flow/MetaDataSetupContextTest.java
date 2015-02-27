package com.sequenceiq.cloudbreak.service.stack.flow;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;

import reactor.core.Reactor;
import reactor.event.Event;

@Ignore("Rewrite this test!")
public class MetaDataSetupContextTest {
    @InjectMocks
    private MetadataSetupContext underTest = new MetadataSetupContext();

    @Mock
    private StackRepository stackRepository;

    @Mock
    private Map<CloudPlatform, MetadataSetup> metadataSetups;

    @Mock
    private Reactor reactor;

    @Mock
    private MetadataSetup metadataSetup;

    private Stack stack;

    @Before
    public void setUp() {
        underTest = new MetadataSetupContext();
        MockitoAnnotations.initMocks(this);
        stack = new Stack();
        stack.getResources().add(new Resource(ResourceType.CLOUDFORMATION_STACK, "stack", stack, "master"));
    }

    @Test
    public void testSetupMetadata() {
        // GIVEN
        given(metadataSetups.get(CloudPlatform.AZURE)).willReturn(metadataSetup);
        given(stackRepository.findOneWithLists(1L)).willReturn(stack);
        doNothing().when(metadataSetup).setupMetadata(stack);
        // WHEN
        underTest.setupMetadata(CloudPlatform.AZURE, 1L);
        // THEN
        verify(metadataSetup, times(1)).setupMetadata(stack);
        verify(reactor, times(0)).notify(any(ReactorConfig.class), any(Event.class));
    }

    @Test
    public void testSetupMetadataWhenExceptionOccurs() {
        // GIVEN
        given(metadataSetups.get(CloudPlatform.AZURE)).willReturn(metadataSetup);
        given(stackRepository.findOneWithLists(1L)).willReturn(stack);
        doThrow(new IllegalStateException()).when(metadataSetup).setupMetadata(stack);
        // WHEN
        underTest.setupMetadata(CloudPlatform.AZURE, 1L);
        // THEN
        verify(reactor, times(1)).notify(any(ReactorConfig.class), any(Event.class));
    }
}
