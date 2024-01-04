package com.sequenceiq.cloudbreak.domain;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.converter.ImageTypeConverter;
import com.sequenceiq.common.api.type.ImageType;

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

    private Long created = System.currentTimeMillis();

    @Convert(converter = ImageTypeConverter.class)
    private ImageType imageType;

    private String baseParcelUrl;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "customImage", cascade = CascadeType.ALL, orphanRemoval = true)
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
        sb.append("imageType='").append(imageType != null ? imageType.name() : null).append('\'');
        sb.append("customizedImageId='").append(customizedImageId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
