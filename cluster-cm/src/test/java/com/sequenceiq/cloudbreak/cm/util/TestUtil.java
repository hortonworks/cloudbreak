package com.sequenceiq.cloudbreak.cm.util;

import java.util.Set;

import com.cloudera.api.swagger.model.ApiParcel;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;

public class TestUtil {

    public static final String CDH = "CDH";

    public static final String SPARK3 = "SPARK3";

    public static final String FLINK = "FLINK";

    public static final String CDH_VERSION = "7.0.0-1.cdh7.0.0.p0.1376867";

    public static final String CDSW = "CDSW";

    public static final String CDSW_VERSION = "2.0.0.p1.1410896";

    private static final String CM = "CM";

    private static final String CM_VERSION = "7.0.0";

    private TestUtil() { }

    public static Set<ClouderaManagerProduct> clouderaManagerProducts() {
        ClouderaManagerProduct cdhProduct = new ClouderaManagerProduct().withName(CDH).withVersion(CDH_VERSION);
        ClouderaManagerProduct cmProduct = new ClouderaManagerProduct().withName(CM).withVersion(CM_VERSION);
        return Set.of(cdhProduct, nonCdhProduct(), cmProduct);
    }

    public static ClouderaManagerProduct nonCdhProduct() {
        return new ClouderaManagerProduct().withName(CDSW).withVersion(CDSW_VERSION);
    }

    public static ApiParcel apiParcel(String type, String status) {
        switch (type) {
            case "CDH":
                return new ApiParcel().product(CDH).version(CDH_VERSION).stage(status);
            case "CDSW":
                return new ApiParcel().product(CDSW).version(CDSW_VERSION).stage(status);
            default:
                return new ApiParcel().product(type).stage(status);
        }
    }
}
