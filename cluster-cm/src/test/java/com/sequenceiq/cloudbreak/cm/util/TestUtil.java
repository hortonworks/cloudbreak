package com.sequenceiq.cloudbreak.cm.util;

import java.util.Set;

import com.cloudera.api.swagger.model.ApiParcel;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;

public class TestUtil {

    public static final String CDH = "CDH";

    public static final String CDH_VERSION = "7.0.0-1.cdh7.0.0.p0.1376867";

    public static final String CDSW = "CDSW";

    public static final String CDSW_VERSION = "2.0.0.p1.1410896";

    private static final String PARCEL_TEMPLATE = "{\"name\":\"%s\",\"version\":\"%s\","
            + "\"parcel\":\"https://archive.cloudera.com/cdh7/7.0.0/parcels/\"}";

    private static final String CDH_ATTRIBUTES = String.format(PARCEL_TEMPLATE, CDH, CDH_VERSION);

    private static final String CDSW_ATTRIBUTES = String.format(PARCEL_TEMPLATE, CDSW, CDSW_VERSION);

    private static final String CM_ATTRIBUTES = "{\"predefined\":false,\"version\":\"7.0.0\","
            + "\"baseUrl\":\"https://archive.cloudera.com/cm7/7.0.0/redhat7/yum/\","
            + "\"gpgKeyUrl\":\"https://archive.cloudera.com/cm7/7.0.0/redhat7/yum/RPM-GPG-KEY-cloudera\"}";

    private TestUtil() { }

    private static ClusterComponent createClusterComponent(String attributeString, String name, ComponentType componentType, Cluster cluster) {
        Json attributes = new Json(attributeString);
        return new ClusterComponent(componentType, name, attributes, cluster);
    }

    public static Set<ClusterComponent> clusterComponentSet(Cluster cluster) {
        ClusterComponent cdhComponent = createClusterComponent(CDH_ATTRIBUTES, CDH, ComponentType.CDH_PRODUCT_DETAILS, cluster);
        ClusterComponent cdswComponent = createClusterComponent(CDSW_ATTRIBUTES, CDSW, ComponentType.CDH_PRODUCT_DETAILS, cluster);
        ClusterComponent cmComponent = createClusterComponent(CM_ATTRIBUTES, ComponentType.CM_REPO_DETAILS.name(), ComponentType.CM_REPO_DETAILS, cluster);
        return Set.of(cdhComponent, cdswComponent, cmComponent);
    }

    public static Cluster clusterComponents(Cluster cluster) {
        cluster.setComponents(clusterComponentSet(cluster));
        return  cluster;
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
