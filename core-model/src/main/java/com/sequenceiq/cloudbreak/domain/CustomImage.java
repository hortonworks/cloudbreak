package com.sequenceiq.cloudbreak.domain;

import com.sequenceiq.cloudbreak.converter.ImageTypeConverter;
import com.sequenceiq.common.api.type.ImageType;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"resourceCrn"}),
        @UniqueConstraint(columnNames = {"imagecatalog_id", "name"})
})
public class CustomImage implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "customimage_generator")
    @SequenceGenerator(name = "customimage_generator", sequenceName = "customimage_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String customizedImageId;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "imagecatalog_id")
    private ImageCatalog imageCatalog;

    @Column(nullable = false)
    private String resourceCrn;

    private String creator;

    private Long created = System.currentTimeMillis();

    @Convert(converter = ImageTypeConverter.class)
    private ImageType imageType;

    private String baseParcelUrl;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "customImage", cascade = CascadeType.REMOVE)
    private Set<VmImage> vmImage = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCustomizedImageId() {
        return customizedImageId;
    }

    public void setCustomizedImageId(String customizedImageId) {
        this.customizedImageId = customizedImageId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ImageCatalog getImageCatalog() {
        return imageCatalog;
    }

    public void setImageCatalog(ImageCatalog imageCatalog) {
        this.imageCatalog = imageCatalog;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public Long getCreated() {
        return created;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public ImageType getImageType() {
        return imageType;
    }

    public void setImageType(ImageType imageType) {
        this.imageType = imageType;
    }

    public String getBaseParcelUrl() {
        return baseParcelUrl;
    }

    public void setBaseParcelUrl(String baseParcelUrl) {
        this.baseParcelUrl = baseParcelUrl;
    }

    public Set<VmImage> getVmImage() {
        return vmImage;
    }

    public void setVmImage(Set<VmImage> vmImage) {
        this.vmImage = vmImage;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ImageCatalog{");
        sb.append("id='").append(id).append('\'');
        sb.append("name='").append(name).append('\'');
        sb.append("imageType='").append(imageType.name()).append('\'');
        sb.append("customizedImageId='").append(customizedImageId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
