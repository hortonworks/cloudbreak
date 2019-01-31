package com.sequenceiq.cloudbreak.service.hostmetadata;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;

public class HostMetadataServiceTest {

    private static final Long CLUSTER_ID = 1L;

    private static final Long HOST_GROUP_ID = 1L;

    private static final String HOST_NAME = "someHostName";

    @Mock
    private HostMetadataRepository hostMetadataRepository;

    @InjectMocks
    private HostMetadataService underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testUpdateHostMetaDataStatusWhenNoHostMetadataFoundThenNotFoundExceptionComesRegardlessOfTheHostMetadataState() {
        when(hostMetadataRepository.findById(CLUSTER_ID)).thenReturn(Optional.empty());

        int expectedExceptionInvokes = HostMetadataState.values().length;
        int actualExceptionInvokes = 0;

        for (HostMetadataState state : HostMetadataState.values()) {
            try {
                underTest.updateHostMetaDataStatus(CLUSTER_ID, state);
            } catch (NotFoundException expected) {
                actualExceptionInvokes++;
            }
        }
        Assert.assertEquals(expectedExceptionInvokes, actualExceptionInvokes);
        verify(hostMetadataRepository, times(expectedExceptionInvokes)).findById(anyLong());
        verify(hostMetadataRepository, times(expectedExceptionInvokes)).findById(CLUSTER_ID);
    }

    @Test
    public void testUpdateHostMetaDataStatusWhenHostMetadataCanBeFoundInDatabaseThenItShouldBeSaved() {
        for (HostMetadataState state : HostMetadataState.values()) {
            HostMetadata expected = mock(HostMetadata.class);
            when(hostMetadataRepository.findById(CLUSTER_ID)).thenReturn(Optional.of(expected));
            when(hostMetadataRepository.save(expected)).thenReturn(expected);

            HostMetadata actual = underTest.updateHostMetaDataStatus(CLUSTER_ID, state);

            Assert.assertEquals(expected, actual);
            verify(expected, times(1)).setHostMetadataState(state);
        }
    }

}