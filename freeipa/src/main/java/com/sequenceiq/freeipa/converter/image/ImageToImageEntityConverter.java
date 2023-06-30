package com.sequenceiq.freeipa.converter.image;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.entity.ImageEntity;

@Component
public class ImageToImageEntityConverter {

    private static final String FREEIPA_LDAP_AGENT = "freeipa-ldap-agent";

    public ImageEntity convert(String accountId, Image source) {
        ImageEntity imageEntity = new ImageEntity();
        imageEntity.setAccountId(accountId);
        imageEntity.setImageId(source.getUuid());
        imageEntity.setOs(source.getOs());
        imageEntity.setOsType(source.getOsType());
        imageEntity.setDate(source.getDate());
        imageEntity.setLdapAgentVersion(extractLdapAgentVersion(source));
        return imageEntity;
    }

    public String extractLdapAgentVersion(Image image) {
        return extractLdapAgentVersion(image.getPackageVersions());
    }

    public String extractLdapAgentVersion(com.sequenceiq.cloudbreak.cloud.model.Image image) {
        return extractLdapAgentVersion(image.getPackageVersions());
    }

    private String extractLdapAgentVersion(Map<String, String> packageVersions) {
        return packageVersions == null ? null : packageVersions.get(FREEIPA_LDAP_AGENT);
    }
}
