package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.rotaterdscert.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
public class DatahubCertificateCheckerTest {

    @Mock
    private DatabaseCertificateRotationOutdatedDatahubsCollector outdatedDatahubsCollector;

    @Mock
    private DatabaseCertificateRotationAffectedDatahubsCollector affectedDatahubsCollector;

    private DatahubCertificateChecker datahubCertificateChecker;

    @BeforeEach
    public void setUp() {
        datahubCertificateChecker = new DatahubCertificateChecker(outdatedDatahubsCollector, affectedDatahubsCollector);
    }

    @Test
    public void testCollectDatahubsWhichMustBeUpdatedDatalakeStackReturnsFilteredDatahubs() {
        // Arrange
        StackView stack = mock(StackView.class);
        when(stack.getType()).thenReturn(StackType.DATALAKE);
        when(stack.getEnvironmentCrn()).thenReturn("test-environment-crn");

        List<String> outdatedCerts = List.of("datahub1", "datahub2", "datahub3");
        List<String> affectedDatahubs = List.of("datahub2", "datahub3", "datahub4");

        when(outdatedDatahubsCollector.getDatahubNamesWithOutdatedCerts(stack)).thenReturn(outdatedCerts);
        when(affectedDatahubsCollector.collectDatahubNamesWhereCertCheckNecessary("test-environment-crn"))
                .thenReturn(affectedDatahubs);

        List<String> result = datahubCertificateChecker.collectDatahubsWhichMustBeUpdated(stack);

        assertEquals(2, result.size());
        assertTrue(result.contains("datahub2"));
        assertTrue(result.contains("datahub3"));
    }

    @Test
    public void testCollectDatahubsWhichMustBeUpdatedNonDatalakeStackReturnsEmptyList() {
        StackView stack = mock(StackView.class);
        when(stack.getType()).thenReturn(StackType.WORKLOAD);

        List<String> result = datahubCertificateChecker.collectDatahubsWhichMustBeUpdated(stack);

        assertTrue(result.isEmpty());
        verifyNoInteractions(outdatedDatahubsCollector, affectedDatahubsCollector);
    }

    @Test
    public void testCollectDatahubsWhichMustBeUpdatedDatalakeStackNoMatchingDatahubs() {
        StackView stack = mock(StackView.class);
        when(stack.getType()).thenReturn(StackType.DATALAKE);
        when(stack.getEnvironmentCrn()).thenReturn("test-environment-crn");

        List<String> outdatedCerts = List.of("datahub1");
        List<String> affectedDatahubs = List.of("datahub2");

        when(outdatedDatahubsCollector.getDatahubNamesWithOutdatedCerts(stack)).thenReturn(outdatedCerts);
        when(affectedDatahubsCollector.collectDatahubNamesWhereCertCheckNecessary("test-environment-crn"))
                .thenReturn(affectedDatahubs);

        List<String> result = datahubCertificateChecker.collectDatahubsWhichMustBeUpdated(stack);

        assertTrue(result.isEmpty());
    }
}