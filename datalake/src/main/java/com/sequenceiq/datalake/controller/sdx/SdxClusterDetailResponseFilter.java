package com.sequenceiq.datalake.controller.sdx;

import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

/*
 * Responsible for filtering out data from SdxClusterDetailResponse instances.
 * Although it's a simple operation, implemented as a separate class for readability's sake.
 */
public class SdxClusterDetailResponseFilter {

    public static final byte REMOVE_SOLR_DETAILS = 1;

    public static final byte REMOVE_NAMENODE_DETAILS = 2;

    public static final byte REMOVE_WEBHDFS_DETAILS = 4;

    private SdxClusterDetailResponse response;

    private byte filterCriteria;

    private SdxClusterDetailResponseFilter(SdxClusterDetailResponse response) {
        this.response = response;
    }

    public static SdxClusterDetailResponseFilter on(SdxClusterDetailResponse sdxClusterDetailResponse) {
        return new SdxClusterDetailResponseFilter(sdxClusterDetailResponse);
    }

    public SdxClusterDetailResponseFilter apply(byte filterToApply) {
        filterCriteria |= filterToApply;
        return this;
    }

    public SdxClusterDetailResponse filter() {
        if (response.getStackV4Response() != null) {
            response.getStackV4Response().getCluster().getExposedServices().forEach((key, value) -> {
                value.removeIf(srv ->
                    (filterApplied(REMOVE_NAMENODE_DETAILS) && srv.getServiceName().contains("NAMENODE")) ||
                    (filterApplied(REMOVE_SOLR_DETAILS) && srv.getServiceName().contains("SOLR_SERVER")) ||
                    (filterApplied(REMOVE_WEBHDFS_DETAILS) && srv.getServiceName().contains("MASTER"))
                );
            });
        }
        return response;
    }

    private boolean filterApplied(byte filter) {
        return (filterCriteria & filter) == filter;
    }
}
