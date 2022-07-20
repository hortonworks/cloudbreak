package com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.model.ParcelInfo;
import com.sequenceiq.cloudbreak.cluster.model.ParcelStatus;

@ExtendWith(MockitoExtension.class)
public class CmSyncOperationResultEvaluatorServiceTest {

    private static final String INSTALLED_CM_VERSION = "installedCmVersion";

    private static final String PARCEL_1_NAME = "parcel1Name";

    private static final String PARCEL_1_VERSION = "parcel1Version";

    private static final String PARCEL_2_NAME = "parcel2Name";

    private static final String PARCEL_2_VERSION = "parcel2Version";

    private final CmSyncOperationResultEvaluatorService underTest = new CmSyncOperationResultEvaluatorService();

    @Test
    void testEvaluateCmRepoSyncWhenInstalledVersionAndFoundComponentPresentThenSuccess() {
        CmRepoSyncOperationResult cmRepoSyncOperationResult = new CmRepoSyncOperationResult(INSTALLED_CM_VERSION, new ClouderaManagerRepo());

        CmSyncOperationStatus.Builder cmSyncOperationStatusBuilder = underTest.evaluateCmRepoSync(cmRepoSyncOperationResult);

        CmSyncOperationStatus cmSyncOperationStatus = cmSyncOperationStatusBuilder.build();
        assertTrue(cmSyncOperationStatus.hasSucceeded());
        assertEquals(String.format("Reading CM repository version succeeded, the current version of CM is %s.", INSTALLED_CM_VERSION),
                cmSyncOperationStatus.getMessage());
    }

    @Test
    void testEvaluateCmRepoSyncWhenInstalledVersionNotPresentThenCmDownFailure() {
        CmRepoSyncOperationResult cmRepoSyncOperationResult = new CmRepoSyncOperationResult(null, null);

        CmSyncOperationStatus.Builder cmSyncOperationStatusBuilder = underTest.evaluateCmRepoSync(cmRepoSyncOperationResult);

        CmSyncOperationStatus cmSyncOperationStatus = cmSyncOperationStatusBuilder.build();
        assertFalse(cmSyncOperationStatus.hasSucceeded());
        assertEquals("Reading CM repository version failed, it was not possible to retrieve CM version from the server.",
                cmSyncOperationStatus.getMessage());
    }

    @Test
    void testEvaluateCmRepoSyncWhenFoundComponentMissingThenComponentNotFoundFailure() {
        CmRepoSyncOperationResult cmRepoSyncOperationResult = new CmRepoSyncOperationResult(INSTALLED_CM_VERSION, null);

        CmSyncOperationStatus.Builder cmSyncOperationStatusBuilder = underTest.evaluateCmRepoSync(cmRepoSyncOperationResult);

        CmSyncOperationStatus cmSyncOperationStatus = cmSyncOperationStatusBuilder.build();
        assertFalse(cmSyncOperationStatus.hasSucceeded());
        assertEquals(String.format("Reading CM repository version failed, no matching component found on images for CM server version %s.",
                        INSTALLED_CM_VERSION), cmSyncOperationStatus.getMessage());
    }

    @Test
    void testEvaluateParcelSyncWhenVersionsPresentAndMatchingProductsFoundThenSuccess() {
        Set<ParcelInfo> activeParcels = Set.of(
                new ParcelInfo(PARCEL_1_NAME, PARCEL_1_VERSION, ParcelStatus.ACTIVATED),
                new ParcelInfo(PARCEL_2_NAME, PARCEL_2_VERSION, ParcelStatus.ACTIVATED)
        );
        Set<ClouderaManagerProduct> foundCmProducts = Set.of(
                new ClouderaManagerProduct().withName(PARCEL_1_NAME),
                new ClouderaManagerProduct().withName(PARCEL_2_NAME)
        );
        CmParcelSyncOperationResult cmParcelSyncOperationResult = new CmParcelSyncOperationResult(activeParcels, foundCmProducts);

        CmSyncOperationStatus.Builder cmSyncOperationStatusBuilder = underTest.evaluateParcelSync(cmParcelSyncOperationResult);

        CmSyncOperationStatus cmSyncOperationStatus = cmSyncOperationStatusBuilder.build();
        assertTrue(cmSyncOperationStatus.hasSucceeded());
        assertThat(cmSyncOperationStatus.getMessage(),
                containsString("Reading versions of active parcels succeeded, the following active parcels were found on the CM server: "));
        assertThat(cmSyncOperationStatus.getMessage(), containsString(PARCEL_1_NAME));
        assertThat(cmSyncOperationStatus.getMessage(), containsString(PARCEL_2_NAME));
    }

    @Test
    void testEvaluateParcelSyncWhenInstalledVersionsNotPresentThenFailure() {
        Set<ParcelInfo> activeParcels = Set.of();
        Set<ClouderaManagerProduct> foundCmProducts = Set.of();
        CmParcelSyncOperationResult cmParcelSyncOperationResult = new CmParcelSyncOperationResult(activeParcels, foundCmProducts);

        CmSyncOperationStatus.Builder cmSyncOperationStatusBuilder = underTest.evaluateParcelSync(cmParcelSyncOperationResult);

        CmSyncOperationStatus cmSyncOperationStatus = cmSyncOperationStatusBuilder.build();
        assertFalse(cmSyncOperationStatus.hasSucceeded());
        assertEquals("Reading versions of active parcels failed, it was not possible to retrieve versions of active parcels from the CM server.",
                cmSyncOperationStatus.getMessage());
    }

    @Test
    void testEvaluateParcelSyncWhenMoreVersionsPresentThanMatchingProductsFoundThenFailure() {
        Set<ParcelInfo> activeParcels = Set.of(
                new ParcelInfo(PARCEL_1_NAME, PARCEL_1_VERSION, ParcelStatus.ACTIVATED),
                new ParcelInfo(PARCEL_2_NAME, PARCEL_2_VERSION, ParcelStatus.ACTIVATED)
        );
        Set<ClouderaManagerProduct> foundCmProducts = Set.of(new ClouderaManagerProduct().withName(PARCEL_1_NAME));
        CmParcelSyncOperationResult cmParcelSyncOperationResult = new CmParcelSyncOperationResult(activeParcels, foundCmProducts);

        CmSyncOperationStatus.Builder cmSyncOperationStatusBuilder = underTest.evaluateParcelSync(cmParcelSyncOperationResult);

        CmSyncOperationStatus cmSyncOperationStatus = cmSyncOperationStatusBuilder.build();
        assertFalse(cmSyncOperationStatus.hasSucceeded());
        assertEquals(String.format("Reading versions of active parcels failed, the version of some active parcels could not be retrieved from CM server: %s. " +
                                "Parcel versions successfully read: %s.", Set.of(PARCEL_2_NAME), Set.of(PARCEL_1_NAME)), cmSyncOperationStatus.getMessage()
        );
    }

    @Test
    void testEvaluateParcelSyncWhenVersionsPresentButNoMatchingProductsFoundThenFailure() {
        Set<ParcelInfo> activeParcels = Set.of(
                new ParcelInfo(PARCEL_1_NAME, PARCEL_1_VERSION, ParcelStatus.ACTIVATED),
                new ParcelInfo(PARCEL_2_NAME, PARCEL_2_VERSION, ParcelStatus.ACTIVATED)
        );
        Set<ClouderaManagerProduct> foundCmProducts = Set.of();
        CmParcelSyncOperationResult cmParcelSyncOperationResult = new CmParcelSyncOperationResult(activeParcels, foundCmProducts);

        CmSyncOperationStatus.Builder cmSyncOperationStatusBuilder = underTest.evaluateParcelSync(cmParcelSyncOperationResult);

        CmSyncOperationStatus cmSyncOperationStatus = cmSyncOperationStatusBuilder.build();
        assertFalse(cmSyncOperationStatus.hasSucceeded());
        assertThat(cmSyncOperationStatus.getMessage(), containsString(
                "Reading versions of active parcels failed, the version of active parcels that could not be retrieved from CM server:"));
        assertThat(cmSyncOperationStatus.getMessage(), containsString(PARCEL_1_NAME));
        assertThat(cmSyncOperationStatus.getMessage(), containsString(PARCEL_2_NAME));
    }

}
