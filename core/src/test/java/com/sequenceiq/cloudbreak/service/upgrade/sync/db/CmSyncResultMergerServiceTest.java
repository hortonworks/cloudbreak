package com.sequenceiq.cloudbreak.service.upgrade.sync.db;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.ComponentConverter;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmParcelSyncOperationResult;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmRepoSyncOperationResult;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationResult;

@ExtendWith(MockitoExtension.class)
public class CmSyncResultMergerServiceTest {

    private static final String CM_PARCEL_COMPONENT_NAME = "cmParcelComponent";

    private static final String CM_REPO_COMPONENT_NAME = "cmRepoComponent";

    @Mock
    private ComponentConverter componentConverter;

    @InjectMocks
    private CmSyncResultMergerService underTest;

    @Test
    void testMerge() {
        Stack stack = new Stack();
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        CmRepoSyncOperationResult cmRepoSyncOperationResult = new CmRepoSyncOperationResult("", clouderaManagerRepo);
        when(componentConverter.fromClouderaManagerRepo(clouderaManagerRepo, stack)).thenReturn(componentWithName(CM_REPO_COMPONENT_NAME));
        Set<ClouderaManagerProduct> clouderaManagerProducts = Set.of(new ClouderaManagerProduct());
        CmParcelSyncOperationResult cmParcelSyncOperationResult = new CmParcelSyncOperationResult(Set.of(), clouderaManagerProducts);
        when(componentConverter.fromClouderaManagerProductList(clouderaManagerProducts, stack)).thenReturn(Set.of(componentWithName(CM_PARCEL_COMPONENT_NAME)));
        CmSyncOperationResult cmSyncOperationResult = new CmSyncOperationResult(cmRepoSyncOperationResult, cmParcelSyncOperationResult);

        Set<Component> mergedComponents = underTest.merge(stack, cmSyncOperationResult);

        assertThat(mergedComponents, hasSize(2));
        assertThat(mergedComponents, containsInAnyOrder(
                hasProperty("name", is(CM_REPO_COMPONENT_NAME)),
                hasProperty("name", is(CM_PARCEL_COMPONENT_NAME))
        ));
        verify(componentConverter).fromClouderaManagerRepo(clouderaManagerRepo, stack);
        verify(componentConverter).fromClouderaManagerProductList(clouderaManagerProducts, stack);
    }

    @Test
    void testMergeNoClouderaManagerRepo() {
        Stack stack = new Stack();
        CmRepoSyncOperationResult cmRepoSyncOperationResult = new CmRepoSyncOperationResult(null, null);
        Set<ClouderaManagerProduct> clouderaManagerProducts = Set.of(new ClouderaManagerProduct());
        CmParcelSyncOperationResult cmParcelSyncOperationResult = new CmParcelSyncOperationResult(Set.of(), clouderaManagerProducts);
        when(componentConverter.fromClouderaManagerProductList(clouderaManagerProducts, stack)).thenReturn(Set.of(componentWithName(CM_PARCEL_COMPONENT_NAME)));
        CmSyncOperationResult cmSyncOperationResult = new CmSyncOperationResult(cmRepoSyncOperationResult, cmParcelSyncOperationResult);

        Set<Component> mergedComponents = underTest.merge(stack, cmSyncOperationResult);

        assertThat(mergedComponents, hasSize(1));
        assertThat(mergedComponents, contains(hasProperty("name", is(CM_PARCEL_COMPONENT_NAME))));
        verify(componentConverter, never()).fromClouderaManagerRepo(any(), any());
        verify(componentConverter).fromClouderaManagerProductList(clouderaManagerProducts, stack);
    }

    @Test
    void testMergeWhenNoClouderaManagerProductFound() {
        Stack stack = new Stack();
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        CmRepoSyncOperationResult cmRepoSyncOperationResult = new CmRepoSyncOperationResult("", clouderaManagerRepo);
        when(componentConverter.fromClouderaManagerRepo(clouderaManagerRepo, stack)).thenReturn(componentWithName(CM_REPO_COMPONENT_NAME));
        CmParcelSyncOperationResult cmParcelSyncOperationResult = new CmParcelSyncOperationResult(Set.of(), Set.of());
        CmSyncOperationResult cmSyncOperationResult = new CmSyncOperationResult(cmRepoSyncOperationResult, cmParcelSyncOperationResult);

        Set<Component> mergedComponents = underTest.merge(stack, cmSyncOperationResult);

        assertThat(mergedComponents, hasSize(1));
        assertThat(mergedComponents, contains(hasProperty("name", is(CM_REPO_COMPONENT_NAME))));
        verify(componentConverter).fromClouderaManagerRepo(clouderaManagerRepo, stack);
        verify(componentConverter).fromClouderaManagerProductList(Set.of(), stack);
    }

    @Test
    void testMergeWhenNoResultsAtAll() {
        Stack stack = new Stack();
        CmRepoSyncOperationResult cmRepoSyncOperationResult = new CmRepoSyncOperationResult(null, null);
        CmParcelSyncOperationResult cmParcelSyncOperationResult = new CmParcelSyncOperationResult(Set.of(), Set.of());
        CmSyncOperationResult cmSyncOperationResult = new CmSyncOperationResult(cmRepoSyncOperationResult, cmParcelSyncOperationResult);

        Set<Component> mergedComponents = underTest.merge(stack, cmSyncOperationResult);

        assertThat(mergedComponents, emptyCollectionOf(Component.class));
        verify(componentConverter, never()).fromClouderaManagerRepo(any(), any());
        verify(componentConverter).fromClouderaManagerProductList(Set.of(), stack);
    }

    private Component componentWithName(String name) {
        Component component = new Component();
        component.setName(name);
        return component;
    }

}
