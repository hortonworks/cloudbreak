package com.sequenceiq.freeipa.converter.image;

import org.springframework.stereotype.Component;

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
        return imageEntity;
    }

    public String extractLdapAgentVersion(Image image) {
        return image.getPackageVersions() == null ? null : image.getPackageVersions().get("freeipa-ldap-agent");
    }
}
