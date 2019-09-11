package com.sequenceiq.it.util.cleanup;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiParcel;
import com.cloudera.api.swagger.model.ApiParcelList;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.mock.model.ClouderaManagerMock;

@Component
public class ParcelMockActivatorUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParcelMockActivatorUtil.class);

    @Inject
    private ParcelGeneratorUtil parcelGeneratorUtil;

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
        mockActivateParcels(testContext, clusterName, List.of(parcelGeneratorUtil.getActivatedCDHParcel()));
    }

    /**
     * Mocks the response for the api parcels and provides the given parcels as an answer for such a call.
     * The method does not validate the parcel's content, so you should provide a properly parameterized <code>ApiParcel</code> instance.
     *
     * @param testContext     The mocked test context where the test runs. Should not be null.
     * @param clusterName     The name of the given cluster. It's mandatory for the successful mocking.
     * @param activatedParcel The list of <code>ApiParcel</code> instances which should be parameterized properly. Shouldn't be null or empty.
     * @throws TestFailException        If the activated parcel list is null or empty, then we're unable to mock the expected call,
     * @throws IllegalArgumentException If the test context is null then no operation can be done, so this kind of exception invokes to signify the misusage of
     *                                  the method
     */
    public void mockActivateParcels(MockedTestContext testContext, String clusterName, List<ApiParcel> activatedParcel) {
        validateTestContext(testContext);
        checkClusterName(clusterName);
        if (activatedParcel != null && !activatedParcel.isEmpty()) {
            String path = ClouderaManagerMock.API_ROOT + "/clusters/" + clusterName + "/parcels";
            testContext.getModel().getClouderaManagerMock().getDynamicRouteStack().clearGet(path);
            testContext.getModel().getClouderaManagerMock().getDynamicRouteStack().get(path, (request, response, model) -> {
                ApiParcelList list = new ApiParcelList();
                activatedParcel.forEach(list::addItemsItem);
                return list;
            });
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

}
