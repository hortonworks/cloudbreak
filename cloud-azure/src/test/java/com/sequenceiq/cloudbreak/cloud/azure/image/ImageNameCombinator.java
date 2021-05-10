package com.sequenceiq.cloudbreak.cloud.azure.image;

import java.util.List;

public class ImageNameCombinator {

    private List<String> freeipaSourceBlobs = List.of(
            "https://cldrwestus.blob.core.windows.net/images/freeipa-cdh--2008121423.vhd",
            "https://cldrwestus.blob.core.windows.net/images/freeipa-cdh--2010061458.vhd"
    );

    private List<String> cdhSourceBlobs = List.of(
            "https://cldrwestus.blob.core.windows.net/images/cb-cdh--2012021538.vhd",
            "https://cldrwestus.blob.core.windows.net/images/cb-cdh--2012010839.vhd",
            "https://cldrwestus.blob.core.windows.net/images/cb-cdh--2011300854.vhd",
            "https://cldrwestus.blob.core.windows.net/images/cb-cdh--2011261322.vhd",
            "https://cldrwestus.blob.core.windows.net/images/cb-cdh--2011251004.vhd",
            "https://cldrwestus.blob.core.windows.net/images/cb-cdh--2011240904.vhd",
            "https://cldrwestus.blob.core.windows.net/images/cb-cdh--2011191715.vhd",
            "https://cldrwestus.blob.core.windows.net/images/cb-cdh--2011191044.vhd",
            "https://cldrwestus.blob.core.windows.net/images/cb-cdh--2011171355.vhd",
            "https://cldrwestus.blob.core.windows.net/images/cb-cdh--2011161934.vhd"
    );

    private List<String> imageSourceList;

    private String destinationPrefix;

    private int totalCount;

    public ImageNameCombinator(boolean useFreeipa, String destinationPrefix, int totalCount) {
        this.imageSourceList = useFreeipa ? freeipaSourceBlobs : cdhSourceBlobs;
        this.destinationPrefix = destinationPrefix;
        this.totalCount = totalCount;
    }

    public String getSource(int id) {
        return imageSourceList.get( id % imageSourceList.size() );
    }

    public String getDestinationFilename(int id) {
        return String.format("%s%d_%d.vhd", destinationPrefix, totalCount, id);
    }
}
