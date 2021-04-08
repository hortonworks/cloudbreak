package com.sequenceiq.cloudbreak.cloud.azure.image.marketplace;

public class AzureImageTerms {

    private String id;

    private String name;

    private String type;

    private TermsProperties properties;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public TermsProperties getProperties() {
        return properties;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setProperties(TermsProperties properties) {
        this.properties = properties;
    }

    public static class TermsProperties {

        private boolean accepted;

        private String licenseTextLink;

        private String product;

        private String publisher;

        private String retrieveDatetime;

        private String signature;

        public boolean isAccepted() {
            return accepted;
        }

        public String getLicenseTextLink() {
            return licenseTextLink;
        }

        public String getProduct() {
            return product;
        }

        public String getPublisher() {
            return publisher;
        }

        public String getRetrieveDatetime() {
            return retrieveDatetime;
        }

        public String getSignature() {
            return signature;
        }

        public void setAccepted(boolean accepted) {
            this.accepted = accepted;
        }

        public void setLicenseTextLink(String licenseTextLink) {
            this.licenseTextLink = licenseTextLink;
        }

        public void setProduct(String product) {
            this.product = product;
        }

        public void setPublisher(String publisher) {
            this.publisher = publisher;
        }

        public void setRetrieveDatetime(String retrieveDatetime) {
            this.retrieveDatetime = retrieveDatetime;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }

    }

}