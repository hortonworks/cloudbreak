package com.sequenceiq.cloudbreak.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@ExtendWith(MockitoExtension.class)
class StackDtoDelegateTest {

    @Mock
    StackDtoDelegate stackDtoDelegate;

    @Mock
    Cluster cluster;

    @Mock
    Blueprint blueprint;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void returnsClusterBlueprintTextIfNotNull() {
        when(cluster.getExtendedBlueprintText()).thenReturn("cluster-json");
        when(stackDtoDelegate.getCluster()).thenReturn(cluster);
        doCallRealMethod().when(stackDtoDelegate).getBlueprintJsonText();

        assertEquals("cluster-json", stackDtoDelegate.getBlueprintJsonText());
    }

    @Test
    void returnsBlueprintJsonTextIfClusterTextIsNull() {
        when(cluster.getExtendedBlueprintText()).thenReturn(null);
        when(stackDtoDelegate.getCluster()).thenReturn(cluster);
        when(stackDtoDelegate.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getBlueprintJsonText()).thenReturn("blueprint-json");
        doCallRealMethod().when(stackDtoDelegate).getBlueprintJsonText();

        assertEquals("blueprint-json", stackDtoDelegate.getBlueprintJsonText());
    }

    @Test
    void returnsNullIfBothAreNull() {
        when(cluster.getExtendedBlueprintText()).thenReturn(null);
        when(stackDtoDelegate.getCluster()).thenReturn(cluster);
        when(stackDtoDelegate.getBlueprint()).thenReturn(null);
        doCallRealMethod().when(stackDtoDelegate).getBlueprintJsonText();

        assertNull(stackDtoDelegate.getBlueprintJsonText());
    }

    @Test
    void returnsNullIfClusterNull() {
        when(stackDtoDelegate.getCluster()).thenReturn(null);
        doCallRealMethod().when(stackDtoDelegate).getBlueprintJsonText();

        assertNull(stackDtoDelegate.getBlueprintJsonText());
    }
}