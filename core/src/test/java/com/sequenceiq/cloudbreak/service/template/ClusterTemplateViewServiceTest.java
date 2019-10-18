package com.sequenceiq.cloudbreak.service.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplateView;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterTemplateViewRepository;

@RunWith(MockitoJUnitRunner.class)
public class ClusterTemplateViewServiceTest {

    private static final Long WORKSPACE_ID = 0L;

    @Rule
    public final ExpectedException expected = ExpectedException.none();

    @Mock
    private ClusterTemplateViewRepository repository;

    @InjectMocks
    private ClusterTemplateViewService underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testPrepareCreation() {
        expected.expect(BadRequestException.class);
        expected.expectMessage("Cluster template creation is not supported");

        underTest.prepareCreation(new ClusterTemplateView());
    }

    @Test
    public void testPrepareDeletion() {
        expected.expect(BadRequestException.class);
        expected.expectMessage("Cluster template deletion is not supported");

        underTest.prepareDeletion(new ClusterTemplateView());
    }

    @Test
    public void testWhenFindAllAvailableViewInWorkspaceIsCalledThenItsResultSetShouldBeReturnedWithoutFiltering() {
        ClusterTemplateView repositoryResult = new ClusterTemplateView();
        Set<ClusterTemplateView> resultSetFromRepository = Set.of(repositoryResult);
        when(repository.findAllActive(WORKSPACE_ID)).thenReturn(resultSetFromRepository);

        Set<ClusterTemplateView> result = underTest.findAllActive(WORKSPACE_ID);

        assertNotNull(result);
        assertEquals(resultSetFromRepository.size(), result.size());
        assertTrue(resultSetFromRepository.containsAll(result));

        verify(repository, times(1)).findAllActive(anyLong());
        verify(repository, times(1)).findAllActive(WORKSPACE_ID);
    }

}
