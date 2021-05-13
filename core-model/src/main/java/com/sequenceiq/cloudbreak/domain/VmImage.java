package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"customimage_id", "region"}))
public class VmImage implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "vmimage_generator")
    @SequenceGenerator(name = "vmimage_generator", sequenceName = "vmimage_id_seq", allocationSize = 1)
    private Long id;

    private String creator;

    private Long created = System.currentTimeMillis();

    @ManyToOne
    @JoinColumn(name = "customimage_id")
    private CustomImage customImage;

    @Column(nullable = false)
    private String region;

    @Column(nullable = false)
    private String imageReference;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public Long getCreated() {
        return created;
    }

    public CustomImage getCustomImage() {
        return customImage;
    }

    public void setCustomImage(CustomImage customImage) {
        this.customImage = customImage;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getImageReference() {
        return imageReference;
    }

    public void setImageReference(String imageReference) {
        this.imageReference = imageReference;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("VmImage{");
        sb.append("id='").append(id).append('\'');
        sb.append("region='").append(region).append('\'');
        sb.append("imageReference='").append(imageReference).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
