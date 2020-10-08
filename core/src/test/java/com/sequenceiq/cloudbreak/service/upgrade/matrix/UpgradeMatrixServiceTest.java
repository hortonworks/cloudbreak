package com.sequenceiq.cloudbreak.service.upgrade.matrix;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpgradeMatrixServiceTest {

    @InjectMocks
    private UpgradeMatrixService underTest;

    @Mock
    private UpgradeMatrixDefinition upgradeMatrixDefinition;

    @Test
    public void testIsPermittedByUpgradeMatrixShouldReturnTrueWhenTheUpgradeIsEnabledForThatVersion() {
        when(upgradeMatrixDefinition.getRuntimeUpgradeMatrix()).thenReturn(createRuntimeUpgradeMatrix());

        assertTrue(underTest.permitByUpgradeMatrix("7.1.0", "7.2.2"));
        assertTrue(underTest.permitByUpgradeMatrix("7.2.1", "7.2.2"));
        assertTrue(underTest.permitByUpgradeMatrix("7.2.9", "7.2.3"));
        assertTrue(underTest.permitByUpgradeMatrix("7.2.1", "7.2.4"));
        assertTrue(underTest.permitByUpgradeMatrix("7.2.5", "7.3.5"));
    }

    @Test
    public void testIsPermittedByUpgradeMatrixShouldReturnFalseWhenTheUpgradeIsNotEnabledForThatVersion() {
        when(upgradeMatrixDefinition.getRuntimeUpgradeMatrix()).thenReturn(createRuntimeUpgradeMatrix());

        assertFalse(underTest.permitByUpgradeMatrix("7.1.1", "7.2.2"));
        assertFalse(underTest.permitByUpgradeMatrix("7.0.1", "7.2.3"));
        assertFalse(underTest.permitByUpgradeMatrix("7.3.9", "7.2.4"));
        assertFalse(underTest.permitByUpgradeMatrix("7.2.6", "7.3.5"));
        assertFalse(underTest.permitByUpgradeMatrix("8.5.0", "8.5.5"));
    }

    @Test
    public void testIsPermittedByUpgradeMatrixShouldReturnFalseTheExpressionInTheMatrixIsNotValid() {
        when(upgradeMatrixDefinition.getRuntimeUpgradeMatrix()).thenReturn(createInvalidRuntimeUpgradeMatrix());

        assertFalse(underTest.permitByUpgradeMatrix("7.1.0", "7.2.2"));
    }

    private Set<RuntimeUpgradeMatrix> createRuntimeUpgradeMatrix() {
        return Set.of(
                createUpgradeMatrix("7.2.2", Set.of("7.1.0", "7.2.*")),
                createUpgradeMatrix("7.2.[3,4]", Set.of("7.2.*")),
                createUpgradeMatrix("7.3.*", Set.of("7.2.5")));
    }

    private Set<RuntimeUpgradeMatrix> createInvalidRuntimeUpgradeMatrix() {
        return Set.of(createUpgradeMatrix("7.2.2", Set.of("{7{.1.[3,4]")));
    }

    private RuntimeUpgradeMatrix createUpgradeMatrix(String target, Set<String> source) {
        return new RuntimeUpgradeMatrix(new Runtime(target), source.stream().map(Runtime::new).collect(Collectors.toSet()));
    }
}