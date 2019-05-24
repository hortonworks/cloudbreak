package com.sequenceiq.redbeams.converter.database;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.redbeams.service.crn.CrnService;
import com.sequenceiq.redbeams.service.crn.CrnServiceTest;

public class CrnConverterTest {

    private static final String RESOURCE_NAME = "resourceName";

    @Mock
    private CrnService crnService;

    @InjectMocks
    private CrnConverter underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        underTest.init();
    }

    @Test
    public void testConvertToDbField() {
        Crn crn = CrnServiceTest.getValidCrn();

        String dbField = underTest.convertToDatabaseColumn(crn);

        assertEquals(crn.getResource(), dbField);
    }

    @Test
    public void testConvertToEntityAttribute() {
        underTest.convertToEntityAttribute(RESOURCE_NAME);

        verify(crnService).createDatabaseCrnFrom(RESOURCE_NAME);
    }
}
