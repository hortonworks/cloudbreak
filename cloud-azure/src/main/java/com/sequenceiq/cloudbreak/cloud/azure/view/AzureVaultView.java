package com.sequenceiq.cloudbreak.cloud.azure.view;

public class AzureVaultView {

    private String vaultName;

    private String vaultSecretName;

    private String vaultResourceGroupName;

    private String vaultSecretVersion;

    private boolean enableVault;

    public AzureVaultView(String vaultName, String vaultSecretName, String vaultResourceGroupName, String vaultSecretVersion, boolean enableVault) {
        this.vaultName = vaultName;
        this.vaultSecretName = vaultSecretName;
        this.vaultResourceGroupName = vaultResourceGroupName;
        this.vaultSecretVersion = vaultSecretVersion;
        this.enableVault = enableVault;
    }

    public AzureVaultView() {
        this.vaultName = "";
        this.vaultSecretName = "";
        this.vaultResourceGroupName = "";
        this.vaultSecretVersion = "";
        this.enableVault = false;
    }

    public String getVaultName() {
        return vaultName;
    }

    public String getVaultSecretName() {
        return vaultSecretName;
    }

    public String getVaultResourceGroupName() {
        return vaultResourceGroupName;
    }

    public String getVaultSecretVersion() {
        return vaultSecretVersion;
    }

    public boolean isEnableVault() {
        return enableVault;
    }
}