package com.sequenceiq.cloudbreak.service.datalake;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.repository.cluster.DatalakeResourcesRepository;

public class DatalakeResourcesServiceTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final Long ENVIRONMENT_ID = 1L;

    private static final Long STACK_ID = 1L;

    private DatalakeResources datalakeResources;

    @Mock
    private DatalakeResourcesRepository datalakeResourcesRepository;

    @InjectMocks
    private DatalakeResourcesService underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        datalakeResources = new DatalakeResources();
    }

    @Test
    public void testFindDatalakeResourcesNamesByWorkspaceAndEnvironmentItShouldReturnTheExpectedValueFromRepository() {
        Set<String> expected = Set.of("someStuff");
        when(datalakeResourcesRepository.findDatalakeResourcesNamesByWorkspaceAndEnvironment(WORKSPACE_ID, ENVIRONMENT_ID)).thenReturn(expected);

        Set<String> result = underTest.findDatalakeResourcesNamesByWorkspaceAndEnvironment(WORKSPACE_ID, ENVIRONMENT_ID);

        assertEquals(expected, result);
        verify(datalakeResourcesRepository, times(1)).findDatalakeResourcesNamesByWorkspaceAndEnvironment(anyLong(), anyLong());
        verify(datalakeResourcesRepository, times(1)).findDatalakeResourcesNamesByWorkspaceAndEnvironment(WORKSPACE_ID, ENVIRONMENT_ID);
    }

    @Test
    public void testFindByDatalakeStackIdWhenRepositoryDoesntReturnAnyValueThenEmptyOptionalReturns() {
        when(datalakeResourcesRepository.findByDatalakeStackId(STACK_ID)).thenReturn(null);

        Optional<DatalakeResources> result = underTest.findByDatalakeStackId(STACK_ID);

        assertFalse(result.isPresent());
        verify(datalakeResourcesRepository, times(1)).findByDatalakeStackId(anyLong());
        verify(datalakeResourcesRepository, times(1)).findByDatalakeStackId(STACK_ID);
    }

    @Test
    public void testFindByDatalakeStackIdWhenRepositoryDoesReturnAValueThenNotEmptyOptionalReturns() {
        when(datalakeResourcesRepository.findByDatalakeStackId(STACK_ID)).thenReturn(datalakeResources);

        Optional<DatalakeResources> result = underTest.findByDatalakeStackId(STACK_ID);

        assertTrue(result.isPresent());
        assertEquals(datalakeResources, result.get());
        verify(datalakeResourcesRepository, times(1)).findByDatalakeStackId(anyLong());
        verify(datalakeResourcesRepository, times(1)).findByDatalakeStackId(STACK_ID);
    }

    @Test
    public void testDeleteWhenMethodCallHappensThenRepositoryDeleteHappens() {
        underTest.delete(datalakeResources);

        verify(datalakeResourcesRepository, times(1)).delete(any(DatalakeResources.class));
        verify(datalakeResourcesRepository, times(1)).delete(datalakeResources);
    }

    @Test
    public void testSaveWhenMethodCallHappensThenRepositorySaveHappens() {
        underTest.save(datalakeResources);

        verify(datalakeResourcesRepository, times(1)).save(any(DatalakeResources.class));
        verify(datalakeResourcesRepository, times(1)).save(datalakeResources);
    }

    @Test
    public void testFindByIdWhenMethodCallHappensThenRepositoryCallHappens() {
        when(datalakeResourcesRepository.findById(STACK_ID)).thenReturn(Optional.of(datalakeResources));

        Optional<DatalakeResources> result = underTest.findById(STACK_ID);

        assertTrue(result.isPresent());

        verify(datalakeResourcesRepository, times(1)).findById(anyLong());
        verify(datalakeResourcesRepository, times(1)).findById(STACK_ID);
    }

}