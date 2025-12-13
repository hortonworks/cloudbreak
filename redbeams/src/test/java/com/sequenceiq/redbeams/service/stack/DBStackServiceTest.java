package com.sequenceiq.redbeams.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.repository.DBStackRepository;

@ExtendWith(MockitoExtension.class)
class DBStackServiceTest {

    @Mock
    private DBStackRepository dbStackRepository;

    @InjectMocks
    private DBStackService underTest;

    private DBStack dbStack;

    @BeforeEach
    public void setUp() throws Exception {
        dbStack = new DBStack();
    }

    @Test
    void testGetById() {
        when(dbStackRepository.findById(1L)).thenReturn(Optional.of(dbStack));

        assertEquals(dbStack, underTest.getById(1L));
    }

    @Test
    void testGetByIdNotFound() {
        when(dbStackRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> underTest.getById(1L));
    }

    @Test
    void testGetByNameAndEnvironmentId() {
        when(dbStackRepository.findByNameAndEnvironmentId("mystack", "myenv")).thenReturn(Optional.of(dbStack));

        assertEquals(dbStack, underTest.getByNameAndEnvironmentCrn("mystack", "myenv"));
    }

    @Test
    void testGetByNameAndEnvironmentIdNotFound() {
        when(dbStackRepository.findByNameAndEnvironmentId("mystack", "myenv")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> underTest.getByNameAndEnvironmentCrn("mystack", "myenv"));
    }

    @Test
    void testSave() {
        when(dbStackRepository.save(dbStack)).thenReturn(dbStack);

        assertEquals(dbStack, underTest.save(dbStack));
        verify(dbStackRepository).save(dbStack);
    }

    @Test
    void testFindAllForAutoSync() {
        Set<JobResource> expected = Collections.emptySet();
        when(dbStackRepository.findAllDbStackByStatusIn(Status.getAutoSyncStatuses())).thenReturn(expected);

        assertEquals(expected, underTest.findAllForAutoSync());


    }
}
