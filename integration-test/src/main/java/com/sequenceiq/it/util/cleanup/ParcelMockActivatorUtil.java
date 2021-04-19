package com.sequenceiq.it.util.cleanup;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiParcel;
import com.cloudera.api.swagger.model.ApiParcelList;
import com.cloudera.api.swagger.model.ApiParcelState;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHInfo;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.config.InTestCdhParcelProvider;

@Component
public class ParcelMockActivatorUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParcelMockActivatorUtil.class);

    @Inject
    private ParcelGeneratorUtil parcelGeneratorUtil;

    @Inject
    private InTestCdhParcelProvider inTestCdhParcelProvider;

    public void mockActivateWithDefaultParcels(MockedTestContext testContext, String clusterName, ApiParcel... parcels) {
        List<ApiParcel> apiParcels = getDefaultApiParcels(testContext);
        apiParcels.addAll(Arrays.asList(parcels));
        mockActivateParcels(testContext, clusterName, apiParcels);
    }

    private List<ApiParcel> getDefaultApiParcels(MockedTestContext testContext) {
        String blueprintCdhVersion = testContext.getCloudProvider().getBlueprintCdhVersion();
        DefaultCDHInfo defaultCDHInfo = inTestCdhParcelProvider
                .getParcels(blueprintCdhVersion)
                .get(blueprintCdhVersion);
        return defaultCDHInfo.getParcels().stream().map(product -> {
            ApiParcel apiParcel = new ApiParcel();
            apiParcel.setVersion(product.getVersion());
            apiParcel.setProduct(product.getName());
            apiParcel.setStage("ACTIVATED");
            ApiParcelState state = new ApiParcelState();
            state.setCount(0);
            apiParcel.setState(state);
            return apiParcel;
        }).collect(Collectors.toList());
    }

    /**
     * Mocks the response for the api parcels and provides a default answer for such a call.
     *
     * @param testContext The mocked test context where the test runs. Should not be null.
     * @param clusterName The name of the given cluster. It's mandatory for the successful mocking.
     * @throws TestFailException        If the activated parcel list is null or empty, then we're unable to mock the expected call,
     * @throws IllegalArgumentException If the test context is null then no operation can be done, so this kind of exception invokes to signify the misusage of
     *                                  the method
     */
    public void mockActivateParcels(MockedTestContext testContext, String clusterName) {
        mockActivateParcels(testContext, clusterName, parcelGeneratorUtil.getActivatedCDHParcel());
    }

    /**
     * Mocks the response for the api parcels and provides the given parcel as an answer for such a call.
     * The method does not validate the parcel's content, so you should provide a properly parameterized <code>ApiParcel</code> instance.
     *
     * @param testContext     The mocked test context where the test runs. Should not be null.
     * @param clusterName     The name of the given cluster. It's mandatory for the successful mocking.
     * @param activatedParcel The <code>ApiParcel</code> instance which should be parameterized properly. Shouldn't be null.
     * @throws TestFailException        If the activated parcel is null, then we're unable to mock the expected call.
     * @throws IllegalArgumentException If the test context is null then no operation can be done, so this kind of exception invokes to signify the misusage of
     *                                  the method
     */
    public void mockActivateParcel(MockedTestContext testContext, String clusterName, ApiParcel activatedParcel) {
        validateTestContext(testContext);
        checkClusterName(clusterName);
        if (activatedParcel != null) {
//            String path = getPathForGetParcels(clusterName);
//            testContext.getModel().getClouderaManagerMock().getDynamicRouteStack().clearGet(path);
//            testContext.getModel().getClouderaManagerMock().getDynamicRouteStack()
//                    .get(path, (request, response, model) -> getApiParcelListFromSingleParcel(activatedParcel));
        } else {
            throw new TestFailException("If you would like to mock the activated parcel you should specify it!");
        }
    }

    /**
     * Mocks the response for the api parcels and provides the given parcels as an answer for such a call.
     * The method does not validate the parcel's content, so you should provide a properly parameterized <code>ApiParcel</code> instance.
     *
     * @param testContext      The mocked test context where the test runs. Should not be null.
     * @param clusterName      The name of the given cluster. It's mandatory for the successful mocking.
     * @param activatedParcels The array of <code>ApiParcel</code> instances which should be parameterized properly. Shouldn't be null or empty.
     * @throws TestFailException        If the activated parcel list is null or empty, then we're unable to mock the expected call.
     * @throws IllegalArgumentException If the test context is null then no operation can be done, so this kind of exception invokes to signify the misusage of
     *                                  the method
     */
    public void mockActivateParcels(MockedTestContext testContext, String clusterName, ApiParcel... activatedParcels) {
        validateTestContext(testContext);
        checkClusterName(clusterName);
        if (activatedParcels != null && activatedParcels.length > 0) {
//            String path = getPathForGetParcels(clusterName);
//            testContext.getModel().getClouderaManagerMock().getDynamicRouteStack().clearGet(path);
//            testContext.getModel().getClouderaManagerMock().getDynamicRouteStack()
//                    .get(path, (request, response, model) -> getApiParcelListFromArray(activatedParcels));
        } else {
            throw new TestFailException("If you would like to mock the activated parcels you should specify at least one!");
        }
    }

    /**
     * Mocks the response for the api parcels and provides the given parcels as an answer for such a call.
     * The method does not validate the parcel's content, so you should provide a properly parameterized <code>ApiParcel</code> instance.
     *
     * @param testContext      The mocked test context where the test runs. Should not be null.
     * @param clusterName      The name of the given cluster. It's mandatory for the successful mocking.
     * @param activatedParcels The array of <code>ApiParcel</code> instances which should be parameterized properly. Shouldn't be null or empty.
     * @throws TestFailException        If the activated parcel list is null or empty, then we're unable to mock the expected call.
     * @throws IllegalArgumentException If the test context is null then no operation can be done, so this kind of exception invokes to signify the misusage of
     *                                  the method
     */
    public void mockActivateParcels(MockedTestContext testContext, String clusterName, Collection<ApiParcel> activatedParcels) {
        validateTestContext(testContext);
        checkClusterName(clusterName);
        if (CollectionUtils.isNotEmpty(activatedParcels)) {
//            String path = getPathForGetParcels(clusterName);
//            testContext.getModel().getClouderaManagerMock().getDynamicRouteStack().clearGet(path);
//            testContext.getModel().getClouderaManagerMock().getDynamicRouteStack()
//                    .get(path, (request, response, model) -> getApiParcelListFromCollection(activatedParcels));
        } else {
            throw new TestFailException("If you would like to mock the activated parcels you should specify at least one!");
        }
    }

    private void validateTestContext(MockedTestContext testContext) {
        if (testContext == null) {
            throw new IllegalArgumentException("The test context should not be null!");
        }
    }

    private void checkClusterName(String clusterName) {
        if (StringUtils.isEmpty(clusterName)) {
            LOGGER.warn("cluster name is empty which could lead the api parcel activation mocking invalid!");
        }
    }

    private ApiParcelList getApiParcelListFromSingleParcel(ApiParcel parcel) {
        ApiParcelList list = new ApiParcelList();
        list.addItemsItem(parcel);
        return list;
    }

    private ApiParcelList getApiParcelListFromArray(ApiParcel[] parcels) {
        ApiParcelList list = new ApiParcelList();
        for (ApiParcel parcel : parcels) {
            list.addItemsItem(parcel);
        }
        return list;
    }

    private ApiParcelList getApiParcelListFromCollection(Collection<ApiParcel> parcels) {
        ApiParcelList list = new ApiParcelList();
        for (ApiParcel parcel : parcels) {
            list.addItemsItem(parcel);
        }
        return list;
    }
}
