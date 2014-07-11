package com.sequenceiq.cloudbreak.service.stack.flow;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.Reactor;
import reactor.event.Event;

import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.times;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;


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
    }

    @Test
    public void testSetupMetadata() {
        // GIVEN
        given(metadataSetups.get(CloudPlatform.AZURE)).willReturn(metadataSetup);
        given(stackRepository.findById(1L)).willReturn(stack);
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
        given(stackRepository.findById(1L)).willReturn(stack);
        doThrow(new IllegalStateException()).when(metadataSetup).setupMetadata(stack);
        // WHEN
        underTest.setupMetadata(CloudPlatform.AZURE, 1L);
        // THEN
        verify(reactor, times(1)).notify(any(ReactorConfig.class), any(Event.class));
    }
}
