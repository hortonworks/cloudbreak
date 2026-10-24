package com.sequenceiq.cloudbreak.cloud.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

@ExtendWith(MockitoExtension.class)
class MockResourceConnectorTest {

    @Mock
    private MockUrlFactory mockUrlFactory;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private Invocation.Builder invocationBuilder;

    @Mock
    private CloudResource cloudResource;

    @Captor
    private ArgumentCaptor<Entity<?>> entityCaptor;

    @InjectMocks
    private MockResourceConnector underTest;

    @Test
    void updateTagsShouldPostTagsToMockInfrastructure() {
        Map<String, String> tags = Map.of("key1", "value1");
        when(mockUrlFactory.get(authenticatedContext, "/tags")).thenReturn(invocationBuilder);

        underTest.updateTags(authenticatedContext, List.of(cloudResource), tags);

        verify(invocationBuilder).post(entityCaptor.capture(), eq(String.class));
        assertEquals(tags, entityCaptor.getValue().getEntity());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, entityCaptor.getValue().getMediaType());
    }

    @Test
    void deleteTagsShouldPostTagKeysToMockInfrastructure() {
        Set<String> tagKeys = Set.of("key1");
        when(mockUrlFactory.get(authenticatedContext, "/tags/delete")).thenReturn(invocationBuilder);

        underTest.deleteTags(authenticatedContext, List.of(cloudResource), tagKeys);

        verify(invocationBuilder).post(entityCaptor.capture(), eq(String.class));
        assertEquals(tagKeys, entityCaptor.getValue().getEntity());
    }

    @Test
    void updateTagsWhenTagsEmptyThenNoCall() {
        underTest.updateTags(authenticatedContext, List.of(cloudResource), Map.of());

        verifyNoInteractions(mockUrlFactory);
    }

    @Test
    void deleteTagsWhenTagKeysEmptyThenNoCall() {
        underTest.deleteTags(authenticatedContext, List.of(cloudResource), Set.of());

        verifyNoInteractions(mockUrlFactory);
    }

    @Test
    void updateTagsWhenMockInfrastructureFailsThenThrows() {
        when(mockUrlFactory.get(authenticatedContext, "/tags")).thenReturn(invocationBuilder);
        when(invocationBuilder.post(any(Entity.class), eq(String.class))).thenThrow(new InternalServerErrorException());

        assertThrows(InternalServerErrorException.class,
                () -> underTest.updateTags(authenticatedContext, List.of(cloudResource), Map.of("key1", "value1")));
    }
}
