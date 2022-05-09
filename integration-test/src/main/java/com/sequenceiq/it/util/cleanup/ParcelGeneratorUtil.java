package com.sequenceiq.it.util.cleanup;

import java.math.BigDecimal;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiParcel;
import com.cloudera.api.swagger.model.ApiParcelState;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;

@Component
public class ParcelGeneratorUtil {

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    /**
     * Provides a CDH <code>ApiParcel</code> instance with some predefined values with the status if <code>ACTIVATED</code>.
     *
     * @return a minimal configuration of a CDH ApiParcel instance
     */
    public ApiParcel getActivatedCDHParcel() {
        String runtimeVersion = commonClusterManagerProperties.getRuntimeVersion();
        return getActivatedCDHParcelWithVersion(runtimeVersion.concat("-1.cdh").concat(runtimeVersion).concat(".p0.1454941"));
    }

    public ApiParcel getActivatedCDHParcelWithVersion(String version) {
        ApiParcel parcel = new ApiParcel();
        ApiParcelState state = new ApiParcelState();
        state.setCount(new BigDecimal(0));
        parcel.setProduct("CDH");
        parcel.setState(state);
        parcel.setVersion(version);
        parcel.setStage("ACTIVATED");
        return parcel;
    }

}
