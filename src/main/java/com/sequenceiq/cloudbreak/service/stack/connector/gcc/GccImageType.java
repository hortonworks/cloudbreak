package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

public enum GccImageType {

    //CENTOS6("https://www.googleapis.com/compute/v1/projects/centos-cloud/global/images/centos-6-v20140619"),
    //COREOS_ALPHA("https://www.googleapis.com/compute/v1/projects/coreos-cloud/global/images/coreos-alpha-386-1-0-v20140723"),
    //COREOS_BETA("https://www.googleapis.com/compute/v1/projects/coreos-cloud/global/images/coreos-beta-367-1-0-v20140715"),
    //BACKPORTS_DEBIAN_7("https://www.googleapis.com/compute/v1/projects/debian-cloud/global/images/backports-debian-7-wheezy-v20140619"),
    //DEBIAN_7("https://www.googleapis.com/compute/v1/projects/debian-cloud/global/images/debian-7-wheezy-v20140619"),
    //OPENSUSE("https://www.googleapis.com/compute/v1/projects/opensuse-cloud/global/images/opensuse-13-1-v20140711"),
    //RHEL("https://www.googleapis.com/compute/v1/projects/rhel-cloud/global/images/rhel-6-v20140619"),
    //SLES("https://www.googleapis.com/compute/v1/projects/suse-cloud/global/images/sles-11-sp3-v20140712"),
    DEBIAN_HACK("https://www.googleapis.com/compute/v1/projects/%s/global/images/withoutpamdebian", "withoutpamdebian");

    private final String value;
    private final String imageName;

    private GccImageType(String value, String imageName) {
        this.value = value;
        this.imageName = imageName;
    }

    public String getValue() {
        return value;
    }

    public String getImageName() {
        return imageName;
    }

    public String getAmbariUbuntu(String projectId) {
        return String.format(DEBIAN_HACK.getValue(), projectId);
    }
}
