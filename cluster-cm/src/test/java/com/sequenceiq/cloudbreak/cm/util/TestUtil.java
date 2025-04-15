package com.sequenceiq.cloudbreak.cm.util;

import java.util.Set;

import com.cloudera.api.swagger.model.ApiParcel;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;

public class TestUtil {

    public static final String CDH = "CDH";

    public static final String SPARK3 = "SPARK3";

    public static final String FLINK = "FLINK";

    public static final String CDH_VERSION = "7.0.0-1.cdh7.0.0.p0.1376867";

    public static final String CDSW = "CDSW";

    public static final String CDSW_VERSION = "2.0.0.p1.1410896";

    private static final String CM = "CM";

    private static final String CM_VERSION = "7.0.0";

    private static final String PARCEL_TEMPLATE = "{\"name\":\"%s\",\"version\":\"%s\","
            + "\"parcel\":\"https://archive.cloudera.com/cdh7/7.0.0/parcels/\"}";

    private static final String CDH_ATTRIBUTES = String.format(PARCEL_TEMPLATE, CDH, CDH_VERSION);

    private static final String CDSW_ATTRIBUTES = String.format(PARCEL_TEMPLATE, CDSW, CDSW_VERSION);

    private static final String CM_ATTRIBUTES = "{\"predefined\":false,\"version\":\"7.0.0\","
            + "\"baseUrl\":\"https://archive.cloudera.com/cm7/7.0.0/redhat7/yum/\","
            + "\"gpgKeyUrl\":\"https://archive.cloudera.com/cm7/7.0.0/redhat7/yum/RPM-GPG-KEY-cloudera\"}";

    private TestUtil() { }

    private static ClusterComponentView createClusterComponent(String attributeString, String name, ComponentType componentType, Long clusterId) {
        Json attributes = new Json(attributeString);
        ClusterComponentView clusterComponentView = new ClusterComponentView();
        clusterComponentView.setClusterId(clusterId);
        clusterComponentView.setAttributes(attributes);
        clusterComponentView.setName(name);
        clusterComponentView.setComponentType(componentType);
        return clusterComponentView;
    }

    public static Set<ClusterComponentView> clusterComponentSet(Long clusterId) {
        ClusterComponentView cdhComponent = createClusterComponent(CDH_ATTRIBUTES, CDH, ComponentType.CDH_PRODUCT_DETAILS, clusterId);
        ClusterComponentView cdswComponent = createClusterComponent(CDSW_ATTRIBUTES, CDSW, ComponentType.CDH_PRODUCT_DETAILS, clusterId);
        ClusterComponentView cmComponent = createClusterComponent(CM_ATTRIBUTES, ComponentType.CM_REPO_DETAILS.name(), ComponentType.CM_REPO_DETAILS, clusterId);
        return Set.of(cdhComponent, cdswComponent, cmComponent);
    }

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
