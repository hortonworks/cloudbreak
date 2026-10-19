package com.sequenceiq.it.cloudbreak.cloud.v4;

import com.sequenceiq.common.model.OsType;

public class FreeIpaProperties {

    private String instanceType;

    private String arm64InstanceType;

    private FreeIpaUpgrade upgrade = new FreeIpaUpgrade();

    private Marketplace marketplace = new Marketplace();

    private Rebuild rebuild = new Rebuild();

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public String getArm64InstanceType() {
        return arm64InstanceType;
    }

    public void setArm64InstanceType(String arm64InstanceType) {
        this.arm64InstanceType = arm64InstanceType;
    }

    public FreeIpaUpgrade getUpgrade() {
        return upgrade;
    }

    public void setUpgrade(FreeIpaUpgrade upgrade) {
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
        private OsType destinationOs = OsType.RHEL9;

        private OsType sourceOs = OsType.RHEL9;

        private String catalog;

        public OsType getDestinationOs() {
            return destinationOs;
        }

        public void setDestinationOs(OsType destinationOs) {
            this.destinationOs = destinationOs;
        }

        public OsType getSourceOs() {
            return sourceOs;
        }

        public void setSourceOs(OsType sourceOs) {
            this.sourceOs = sourceOs;
        }

        public String getCatalog() {
            return catalog;
        }

        public void setCatalog(String catalog) {
            this.catalog = catalog;
        }
    }

    public static class Marketplace {

        private MarketplaceUpgrade upgrade;

        public MarketplaceUpgrade getUpgrade() {
            return upgrade;
        }

        public void setUpgrade(MarketplaceUpgrade upgrade) {
            this.upgrade = upgrade;
        }
    }

    public static class MarketplaceUpgrade {
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

    public static final class Rebuild {
        private String fullbackup;

        private String databackup;

        private String imageId;

        private String catalog;

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
