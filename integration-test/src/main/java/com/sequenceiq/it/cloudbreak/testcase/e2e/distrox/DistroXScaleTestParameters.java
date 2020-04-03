package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import java.util.Map;

public class DistroXScaleTestParameters {

    private static final String TIMES = "times";

    private static final String HOSTGROUP = "hostgroup";

    private static final String SCALE_UP = "scale_up";

    private static final String SCALE_DOWN = "scale_down";

    private static final String IMAGE_CATALOG_NAME = "image_catalog";

    private static final String IMAGE_CATALOG_URL = "image_catalog_url";

    private static final String IMAGE_ID = "image_id";

    private static final int DEFAULT_TIMES = 10;

    private static final int DEFAULT_SCALE_UP = 100;

    private static final int DEFAULT_SCALE_DOWN = 10;

    private static final String DEFAULT_HOSTGROUP = "worker";

    private static final String DEFAULT_IMAGE_CATALOG_NAME = "bigscale_catalog";

    private int times;

    private int scaleUp;

    private int scaleDown;

    private String hostgroup;

    private String imageCatalogName;

    private String imageCatalogUrl;

    private String imageId;

    private DistroXScaleTestParameters() {
    }

    DistroXScaleTestParameters(Map<String, String> allParameters) {
        String times = allParameters.get(TIMES);
        String scaleUp = allParameters.get(SCALE_UP);
        String scaleDown = allParameters.get(SCALE_DOWN);
        String hostGroup = allParameters.get(HOSTGROUP);
        String imageCatalogName = allParameters.get(IMAGE_CATALOG_NAME);

        setHostgroup(hostGroup == null ? DEFAULT_HOSTGROUP : hostGroup);
        setScaleUp(scaleUp == null ? DEFAULT_SCALE_UP : Integer.parseInt(scaleUp));
        setScaleDown(scaleDown == null ? DEFAULT_SCALE_DOWN : Integer.parseInt(scaleDown));
        setTimes(times == null ? DEFAULT_TIMES : Integer.parseInt(times));
        setImageCatalogName(imageCatalogName == null ? DEFAULT_IMAGE_CATALOG_NAME : imageCatalogName);

        imageCatalogUrl = allParameters.get(IMAGE_CATALOG_URL);
        imageId = allParameters.get(IMAGE_ID);
    }

    public int getScaleUp() {
        return scaleUp;
    }

    public void setScaleUp(int scaleUp) {
        this.scaleUp = scaleUp;
    }

    public int getScaleDown() {
        return scaleDown;
    }

    public void setScaleDown(int scaleDown) {
        this.scaleDown = scaleDown;
    }

    public int getTimes() {
        return times;
    }

    public void setTimes(int times) {
        this.times = times;
    }

    public String getHostgroup() {
        return hostgroup;
    }

    public void setHostgroup(String hostgroup) {
        this.hostgroup = hostgroup;
    }

    public String getImageCatalogName() {
        return imageCatalogName;
    }

    public void setImageCatalogName(String imageCatalogName) {
        this.imageCatalogName = imageCatalogName;
    }

    public String getImageCatalogUrl() {
        return imageCatalogUrl;
    }

    public void setImageCatalogUrl(String imageCatalogUrl) {
        this.imageCatalogUrl = imageCatalogUrl;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public boolean isImageCatalogConfigured() {
        return imageCatalogUrl != null && imageId != null;
    }
}
