package com.sequenceiq.it.cloudbreak.cloud.v4;

public class MarketplaceProperties {

    private Upgrade upgrade = new Upgrade();

    private Sdx sdx = new Sdx();

    public Upgrade getUpgrade() {
        return upgrade;
    }

    public void setUpgrade(Upgrade upgrade) {
        this.upgrade = upgrade;
    }

    public Sdx getSdx() {
        return sdx;
    }

    public void setSdx(Sdx marketplace) {
        this.sdx = marketplace;
    }

    public static class Upgrade {
        private String imageId;

        private String catalog;

        public String getImageId() {
            return imageId;
        }

        public void setImageId(String imageId) {
            this.imageId = imageId;
        }

        public String getCatalog() {
            return catalog;
        }

        public void setCatalog(String catalog) {
            this.catalog = catalog;
        }
    }

    public static class Sdx {

        private Upgrade upgrade;

        public Upgrade getUpgrade() {
            return upgrade;
        }

        public void setUpgrade(Upgrade upgrade) {
            this.upgrade = upgrade;
        }
    }
}
