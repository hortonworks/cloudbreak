package com.sequenceiq.redbeams.service.stack;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.repository.DBStackRepository;

import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class DBStackServiceTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private DBStackRepository dbStackRepository;

    @InjectMocks
    private DBStackService underTest;

    private DBStack dbStack;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        dbStack = new DBStack();
    }

    @Test
    public void testGetById() {
        when(dbStackRepository.findById(1L)).thenReturn(Optional.of(dbStack));

        assertEquals(dbStack, underTest.getById(1L));
    }

    @Test
    public void testGetByIdNotFound() {
        thrown.expect(NotFoundException.class);
        when(dbStackRepository.findById(1L)).thenReturn(Optional.empty());

        underTest.getById(1L);
    }

    @Test
    public void testSave() {
        when(dbStackRepository.save(dbStack)).thenReturn(dbStack);

        assertEquals(dbStack, underTest.save(dbStack));
        verify(dbStackRepository).save(dbStack);
    }
}
