package com.sequenceiq.freeipa.entity;

import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;

@Entity(name = "image")
@Audited
@AuditTable("image_history")
public class ImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "image_generator")
    @SequenceGenerator(name = "image_generator", sequenceName = "image_id_seq", allocationSize = 1)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @Audited(targetAuditMode = NOT_AUDITED)
    private Stack stack;

    private String imageName;

    private String userdata;

    private String os;

    private String osType;

    private String imageCatalogUrl;

    private String imageId;

    private String imageCatalogName;

    public String getImageName() {
        return imageName;
    }

    public String getOsType() {
        return osType;
    }

    public String getImageCatalogUrl() {
        return imageCatalogUrl;
    }

    public String getImageId() {
        return imageId;
    }

    public String getImageCatalogName() {
        return imageCatalogName;
    }

    public String getOs() {
        return os;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getUserdata() {
        return userdata;
    }

    public void setUserdata(String userdata) {
        this.userdata = userdata;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }

    public void setImageCatalogUrl(String imageCatalogUrl) {
        this.imageCatalogUrl = imageCatalogUrl;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public void setImageCatalogName(String imageCatalogName) {
        this.imageCatalogName = imageCatalogName;
    }

    @Override
    public String toString() {
        return "Image{"
                + "imageName='" + imageName + '\''
                + ", os='" + os + '\''
                + ", osType='" + osType + '\''
                + ", imageCatalogUrl='" + imageCatalogUrl + '\''
                + ", imageId='" + imageId + '\''
                + ", imageCatalogName='" + imageCatalogName + '\''
                + ", userdata=" + userdata + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            ImageEntity that = (ImageEntity) o;
            return isFieldEquals(that);
        }
    }

    private boolean isFieldEquals(ImageEntity that) {
        return Objects.equals(id, that.id)
                && Objects.equals(imageName, that.imageName)
                && Objects.equals(userdata, that.userdata)
                && Objects.equals(os, that.os)
                && Objects.equals(osType, that.osType)
                && Objects.equals(imageCatalogUrl, that.imageCatalogUrl)
                && Objects.equals(imageId, that.imageId)
                && Objects.equals(imageCatalogName, that.imageCatalogName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, imageName, userdata, os, osType, imageCatalogUrl, imageId, imageCatalogName);
    }
}
