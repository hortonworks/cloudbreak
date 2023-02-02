package com.sequenceiq.sdx.api.model;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;

@ExtendWith(MockitoExtension.class)
public class SdxCustomClusterRequestTest {

    @Mock
    private SdxRecipe sdxRecipe;

    private SdxCustomClusterRequest undertest = new SdxCustomClusterRequest();

    @Test
    void testConvertToPairShouldForwardRecipes() {
        when(sdxRecipe.getName()).thenReturn("recipe");
        undertest.setRecipes(Set.of(sdxRecipe));
        Pair<SdxClusterRequest, ImageSettingsV4Request> result = undertest.convertToPair();
        assertEquals("recipe", result.getLeft().getRecipes().stream().findFirst().get().getName());
    }

}