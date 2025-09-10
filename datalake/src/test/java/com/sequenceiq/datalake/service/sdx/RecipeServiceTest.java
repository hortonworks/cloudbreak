package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.datalake.service.sdx.SdxTestUtil.USER_CRN;
import static com.sequenceiq.datalake.service.sdx.SdxTestUtil.getSdxCluster;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.datalake.entity.SdxCluster;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {
    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @InjectMocks
    private RecipeService underTest;

    @Test
    void testRefreshRecipe() {
        SdxCluster sdxCluster = getSdxCluster();
        when(stackV4Endpoint.refreshRecipesInternal(anyLong(), any(), anyString(), anyString())).thenReturn(null);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.refreshRecipes(sdxCluster, null));
        verify(stackV4Endpoint).refreshRecipesInternal(anyLong(), any(), eq(sdxCluster.getClusterName()), eq(USER_CRN));
    }

    @Test
    void testAttachRecipe() {
        SdxCluster sdxCluster = getSdxCluster();
        when(stackV4Endpoint.attachRecipeInternal(anyLong(), any(), anyString(), anyString())).thenReturn(null);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.attachRecipe(sdxCluster, null));
        verify(stackV4Endpoint).attachRecipeInternal(anyLong(), any(), eq(sdxCluster.getClusterName()), eq(USER_CRN));
    }

    @Test
    void testDetachRecipe() {
        SdxCluster sdxCluster = getSdxCluster();
        when(stackV4Endpoint.detachRecipeInternal(anyLong(), any(), anyString(), anyString())).thenReturn(null);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.detachRecipe(sdxCluster, null));
        verify(stackV4Endpoint).detachRecipeInternal(anyLong(), any(), eq(sdxCluster.getClusterName()), eq(USER_CRN));
    }
}