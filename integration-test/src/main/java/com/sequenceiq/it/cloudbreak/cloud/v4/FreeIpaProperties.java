package com.sequenceiq.it.cloudbreak.cloud.v4;

public class FreeIpaProperties {

    private Upgrade upgrade = new Upgrade();

    public Upgrade getUpgrade() {
        return upgrade;
    }

    public void setUpgrade(Upgrade upgrade) {
        this.upgrade = upgrade;
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
}
