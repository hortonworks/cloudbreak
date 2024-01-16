package com.sequenceiq.redbeams.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@ExtendWith(MockitoExtension.class)
class RedbeamsEventParameterFactoryTest {

    private static final String CRN = "crn";

    private static final long RESOURCE_ID = 1L;

    @InjectMocks
    private RedbeamsEventParameterFactory underTest;

    @Mock
    private DBStackService stackService;

    @Mock
    private DBStack stack;

    @Mock
    private Crn userCrn;

    @Test
    void getUserCrnByResourceIdEmpty() {
        Optional<String> result = underTest.getUserCrnByResourceId(RESOURCE_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void getUserCrnByResourceIdValue() {
        when(stackService.getById(RESOURCE_ID)).thenReturn(stack);
        when(stack.getOwnerCrn()).thenReturn(userCrn);
        when(userCrn.toString()).thenReturn(CRN);

        Optional<String> result = underTest.getUserCrnByResourceId(RESOURCE_ID);

        assertEquals(CRN, result.get());
    }

}
