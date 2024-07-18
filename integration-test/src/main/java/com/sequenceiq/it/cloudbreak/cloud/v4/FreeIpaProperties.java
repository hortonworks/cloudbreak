package com.sequenceiq.it.cloudbreak.cloud.v4;

public class FreeIpaProperties {

    private Upgrade upgrade = new Upgrade();

    private Marketplace marketplace = new Marketplace();

    private Rebuild rebuild = new Rebuild();

    public Upgrade getUpgrade() {
        return upgrade;
    }

    public void setUpgrade(Upgrade upgrade) {
        this.upgrade = upgrade;
    }

    public Marketplace getMarketplace() {
        return marketplace;
    }

    public void setMarketplace(Marketplace marketplace) {
        this.marketplace = marketplace;
    }

    public Rebuild getRebuild() {
        return rebuild;
    }

    public void setRebuild(Rebuild rebuild) {
        this.rebuild = rebuild;
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

    public static class Marketplace {

        private Upgrade upgrade;

        public Upgrade getUpgrade() {
            return upgrade;
        }

        public void setUpgrade(Upgrade upgrade) {
            this.upgrade = upgrade;
        }
    }

    public static final class Rebuild {
        private String fullbackup;

        private String databackup;

        public String getFullbackup() {
            return fullbackup;
        }

        public void setFullbackup(String fullbackup) {
            this.fullbackup = fullbackup;
        }

        public String getDatabackup() {
            return databackup;
        }

        public void setDatabackup(String databackup) {
            this.databackup = databackup;
        }
    }
}
