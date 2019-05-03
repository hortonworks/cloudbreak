package com.sequenceiq.freeipa.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

@Entity
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "image_generator")
    @SequenceGenerator(name = "image_generator", sequenceName = "image_id_seq", allocationSize = 1)
    private Long id;

    @OneToOne
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
}
