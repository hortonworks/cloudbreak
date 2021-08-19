package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image;

import java.util.List;
import java.util.Objects;

public class ImageComponentVersions implements Comparable<ImageComponentVersions> {

    private String cm;

    private String cmGBN;

    private String cdp;

    private String cdpGBN;

    private String os;

    private String osPatchLevel;

    private List<ParcelInfoResponse> parcelInfoResponseList;

    public ImageComponentVersions() {
    }

    public ImageComponentVersions(String cm, String cmGBN, String cdp, String cdpGBN, String os,
            String osPatchLevel, List<ParcelInfoResponse> parcelInfoResponseList) {
        this.cm = cm;
        this.cmGBN = cmGBN;
        this.cdp = cdp;
        this.cdpGBN = cdpGBN;
        this.os = os;
        this.osPatchLevel = osPatchLevel;
        this.parcelInfoResponseList = parcelInfoResponseList;
    }

    public String getCm() {
        return cm;
    }

    public void setCm(String cm) {
        this.cm = cm;
    }

    public String getCmGBN() {
        return cmGBN;
    }

    public void setCmGBN(String cmGBN) {
        this.cmGBN = cmGBN;
    }

    public String getCdp() {
        return cdp;
    }

    public void setCdp(String cdp) {
        this.cdp = cdp;
    }

    public String getCdpGBN() {
        return cdpGBN;
    }

    public void setCdpGBN(String cdpGBN) {
        this.cdpGBN = cdpGBN;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getOsPatchLevel() {
        return osPatchLevel;
    }

    public void setOsPatchLevel(String osPatchLevel) {
        this.osPatchLevel = osPatchLevel;
    }

    public List<ParcelInfoResponse> getParcelInfoResponseList() {
        return parcelInfoResponseList;
    }

    public void setParcelInfoResponseList(List<ParcelInfoResponse> parcelInfoResponseList) {
        this.parcelInfoResponseList = parcelInfoResponseList;
    }

    @Override
    public String toString() {
        return "ImageComponentVersions{" +
                "cm='" + cm + '\'' +
                ", cmGBN='" + cmGBN + '\'' +
                ", cdp='" + cdp + '\'' +
                ", cdpGBN='" + cdpGBN + '\'' +
                ", os='" + os + '\'' +
                ", osPatchLevel='" + osPatchLevel + '\'' +
                ", parcelInfoResponseList=" + parcelInfoResponseList +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ImageComponentVersions that = (ImageComponentVersions) o;
        return Objects.equals(cm, that.cm) &&
                Objects.equals(cmGBN, that.cmGBN) &&
                Objects.equals(cdp, that.cdp) &&
                Objects.equals(cdpGBN, that.cdpGBN) &&
                Objects.equals(os, that.os) &&
                Objects.equals(osPatchLevel, that.osPatchLevel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cm, cmGBN, cdp, cdpGBN, os, osPatchLevel);
    }

    //CHECKSTYLE:OFF
    @Override
    public int compareTo(ImageComponentVersions o) {
        int ret = 0;
        if (cm != null) {
            ret = cm.compareTo(o.cm);
        }
        if (ret == 0 && cdp != null) {
            ret = cdp.compareTo(o.cdp);
        }
        if (ret == 0 && cmGBN != null) {
            ret = cmGBN.compareTo(o.cmGBN);
        }
        if (ret == 0 && cdpGBN != null) {
            ret = cdpGBN.compareTo(o.cdpGBN);
        }
        if (ret == 0 && os != null) {
            ret = os.compareTo(o.os);
        }
        if (ret == 0 && osPatchLevel != null) {
            ret = osPatchLevel.compareTo(o.osPatchLevel);
        }
        return ret;
    }
    //CHECKSTYLE:ON
}
