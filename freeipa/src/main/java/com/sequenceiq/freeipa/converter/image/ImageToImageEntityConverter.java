package com.sequenceiq.freeipa.converter.image;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.entity.ImageEntity;

@Component
public class ImageToImageEntityConverter {

    public ImageEntity convert(String accountId, Image source) {
        ImageEntity imageEntity = new ImageEntity();
        imageEntity.setAccountId(accountId);
        imageEntity.setImageId(source.getUuid());
        imageEntity.setOs(source.getOs());
        imageEntity.setOsType(source.getOsType());
        imageEntity.setDate(source.getDate());
        imageEntity.setLdapAgentVersion(extractLdapAgentVersion(source));
        imageEntity.setImdsVersion(extractImdsVersion(source));
        imageEntity.setSaltVersion(extractSaltVersion(source));
        imageEntity.setSourceImage(extractSourceImage(source));
        return imageEntity;
    }

    public String extractImdsVersion(Image image) {
        return extractImdsVersion(image.getPackageVersions());
    }

    public String extractImdsVersion(com.sequenceiq.cloudbreak.cloud.model.Image image) {
        return extractImdsVersion(image.getPackageVersions());
    }

    private String extractImdsVersion(Map<String, String> packageVersions) {
        return packageVersions == null ? null : packageVersions.get(ImagePackageVersion.IMDS_VERSION.getKey());
    }

    public String extractLdapAgentVersion(Image image) {
        return extractLdapAgentVersion(image.getPackageVersions());
    }

    public String extractLdapAgentVersion(com.sequenceiq.cloudbreak.cloud.model.Image image) {
        return extractLdapAgentVersion(image.getPackageVersions());
    }

    private String extractLdapAgentVersion(Map<String, String> packageVersions) {
        return packageVersions == null ? null : packageVersions.get(ImagePackageVersion.FREEIPA_LDAP_AGENT.getKey());
    }

    public String extractSourceImage(Image image) {
        return extractSourceImage(image.getPackageVersions());
    }

    public String extractSourceImage(com.sequenceiq.cloudbreak.cloud.model.Image image) {
        return extractSourceImage(image.getPackageVersions());
    }

    private String extractSourceImage(Map<String, String> packageVersions) {
        return packageVersions == null ? null : packageVersions.get(ImagePackageVersion.SOURCE_IMAGE.getKey());
    }

    public String extractSaltVersion(Image image) {
        return extractSaltVersion(image.getPackageVersions());
    }

    private String extractSaltVersion(Map<String, String> packageVersions) {
        return packageVersions == null ? null : packageVersions.get(ImagePackageVersion.SALT.getKey());
    }
}
