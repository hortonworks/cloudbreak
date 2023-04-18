package com.sequenceiq.cloudbreak.cloud.azure.image.marketplace;

import java.util.StringJoiner;

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

    @Override
    public String toString() {
        return new StringJoiner(", ", AzureImageTerms.class.getSimpleName() + "[", "]")
                .add("id='" + id + "'")
                .add("name='" + name + "'")
                .add("type='" + type + "'")
                .add("properties=" + properties)
                .toString();
    }

    public static class TermsProperties {

        private boolean accepted;

        private String licenseTextLink;

        private String product;

        private String publisher;

        private String retrieveDatetime;

        private String signature;

        private String plan;

        private String privacyPolicyLink;

        private String marketplaceTermsLink;

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

        public String getPlan() {
            return plan;
        }

        public void setPlan(String plan) {
            this.plan = plan;
        }

        public String getPrivacyPolicyLink() {
            return privacyPolicyLink;
        }

        public void setPrivacyPolicyLink(String privacyPolicyLink) {
            this.privacyPolicyLink = privacyPolicyLink;
        }

        public String getMarketplaceTermsLink() {
            return marketplaceTermsLink;
        }

        public void setMarketplaceTermsLink(String marketplaceTermsLink) {
            this.marketplaceTermsLink = marketplaceTermsLink;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", TermsProperties.class.getSimpleName() + "[", "]")
                    .add("accepted=" + accepted)
                    .add("licenseTextLink='" + licenseTextLink + "'")
                    .add("product='" + product + "'")
                    .add("publisher='" + publisher + "'")
                    .add("retrieveDatetime='" + retrieveDatetime + "'")
                    .add("signature='" + signature + "'")
                    .add("plan='" + plan + "'")
                    .add("privacyPolicyLink='" + privacyPolicyLink + "'")
                    .add("marketplaceTermsLink='" + marketplaceTermsLink + "'")
                    .toString();
        }
    }
}