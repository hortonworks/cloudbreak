package com.sequenceiq.cloudbreak.service.upgrade.image.locked;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

@ExtendWith(MockitoExtension.class)
class LockedComponentCheckerTest {

    private final Image candidateImage = mock(Image.class);

    private final Image currentImage = mock(Image.class);

    private final Map<String, String> activatedParcels = Map.of();

    @Mock
    private ParcelMatcher parcelMatcher;

    @Mock
    private StackVersionMatcher stackVersionMatcher;

    @Mock
    private CmVersionMatcher cmVersionMatcher;

    @InjectMocks
    private LockedComponentChecker underTest;

    static Object[][] parameters() {
        return new Object[][]{
                {Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE},
                {Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, Boolean.TRUE},
                {Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.TRUE},
                {Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE},
                {Boolean.FALSE, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE},
                {Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, Boolean.FALSE},
                {Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE},
                {Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE}
        };
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testResult(Boolean expectedResult, Boolean parcelMatching, Boolean stackVersionMatching, Boolean cmVersionMatching) {
        when(parcelMatcher.isMatchingNonCdhParcels(candidateImage, activatedParcels)).thenReturn(parcelMatching);
        when(stackVersionMatcher.isMatchingStackVersion(candidateImage, activatedParcels)).thenReturn(stackVersionMatching);
        when(cmVersionMatcher.isCmVersionMatching(currentImage, candidateImage)).thenReturn(cmVersionMatching);

        boolean result = underTest.isUpgradePermitted(currentImage, candidateImage, activatedParcels);

        assertEquals(expectedResult, result);
        verify(parcelMatcher, times(1)).isMatchingNonCdhParcels(candidateImage, activatedParcels);
        verify(stackVersionMatcher, times(1)).isMatchingStackVersion(candidateImage, activatedParcels);
        verify(cmVersionMatcher, times(1)).isCmVersionMatching(currentImage, candidateImage);
    }
}