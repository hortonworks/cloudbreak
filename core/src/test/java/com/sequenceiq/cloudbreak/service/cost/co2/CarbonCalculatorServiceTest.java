package com.sequenceiq.cloudbreak.service.cost.co2;
// CHECKSTYLE:OFF
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CarbonCalculatorServiceTest {

    private static final String TEST_CRN ="crn:cdp:datahub:us-west-1:e7b1345f-4ae1-4594-9113-fc91f22ef8bd:cluster:c7da2918-dd14-49ed-9b43-33ff55bd6309";

    @InjectMocks
    private CarbonCalculatorService underTest;

    @Test
    void getHourlyCarbonFootPrintByCrn() {
        //TODO: add some usecases
        assertEquals(17.4888, underTest.getHourlyCarbonFootPrintByCrn(TEST_CRN));
    }
}