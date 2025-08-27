package com.sequenceiq.freeipa.entity;

import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;

import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.common.dal.model.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;

@Entity(name = "image")
@Audited
@AuditTable("image_history")
public class ImageEntity implements AccountIdAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "image_generator")
    @SequenceGenerator(name = "image_generator", sequenceName = "image_id_seq", allocationSize = 1)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @Audited(targetAuditMode = NOT_AUDITED)
    private Stack stack;

    private String imageName;

    @Deprecated
    private String userdata;

    // The new userdata field which stored in vault
    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret gatewayUserdata = Secret.EMPTY;

    private String os;

    private String osType;

    private String imageCatalogUrl;

    private String imageId;

    private String imageCatalogName;

    @Column(name = "image_date")
    private String date;

    private String ldapAgentVersion;

    private String accountId;

    private String sourceImage;

    private String imdsVersion;

    private String saltVersion;

    private String architecture;

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }

    public String getImageCatalogUrl() {
        return imageCatalogUrl;
    }

    public void setImageCatalogUrl(String imageCatalogUrl) {
        this.imageCatalogUrl = imageCatalogUrl;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getImageCatalogName() {
        return imageCatalogName;
    }

    public void setImageCatalogName(String imageCatalogName) {
        this.imageCatalogName = imageCatalogName;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
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

    public String getUserdata() {
        return userdata;
    }

    public void setUserdata(String userdata) {
        this.userdata = userdata;
    }

    public String getUserdataWrapper() {
        return Strings.isNullOrEmpty(getGatewayUserdata()) ? getUserdata() : getGatewayUserdata();
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getLdapAgentVersion() {
        return ldapAgentVersion;
    }

    public void setLdapAgentVersion(String ldapAgentVersion) {
        this.ldapAgentVersion = ldapAgentVersion;
    }

    public String getGatewayUserdata() {
        return gatewayUserdata.getRaw();
    }

    public void setGatewayUserdata(String gatewayUserdata) {
        if (gatewayUserdata != null) {
            this.gatewayUserdata = new Secret(gatewayUserdata);
        }
    }

    public Secret getGatewayUserdataSecret() {
        return gatewayUserdata;
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getSourceImage() {
        return sourceImage;
    }

    public void setSourceImage(String sourceImage) {
        this.sourceImage = sourceImage;
    }

    public String getImdsVersion() {
        return imdsVersion;
    }

    public void setImdsVersion(String imdsVersion) {
        this.imdsVersion = imdsVersion;
    }

    public String getSaltVersion() {
        return saltVersion;
    }

    public void setSaltVersion(String saltVersion) {
        this.saltVersion = saltVersion;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    @Override
    public String toString() {
        return "ImageEntity{" +
                "id=" + id +
                ", imageName='" + imageName + '\'' +
                ", userdata='" + userdata + '\'' +
                ", os='" + os + '\'' +
                ", osType='" + osType + '\'' +
                ", imageCatalogUrl='" + imageCatalogUrl + '\'' +
                ", imageId='" + imageId + '\'' +
                ", imageCatalogName='" + imageCatalogName + '\'' +
                ", date='" + date + '\'' +
                ", ldapAgentVersion='" + ldapAgentVersion + '\'' +
                ", accountId='" + accountId + '\'' +
                ", sourceImage='" + sourceImage + '\'' +
                ", imdsVersion='" + imdsVersion + '\'' +
                ", saltVersion='" + saltVersion + '\'' +
                ", architecture='" + architecture + '\'' +
                '}';
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

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    private boolean isFieldEquals(ImageEntity that) {
        return Objects.equals(id, that.id)
                && Objects.equals(imageName, that.imageName)
                && Objects.equals(userdata, that.userdata)
                && Objects.equals(os, that.os)
                && Objects.equals(osType, that.osType)
                && Objects.equals(imageCatalogUrl, that.imageCatalogUrl)
                && Objects.equals(imageId, that.imageId)
                && Objects.equals(imageCatalogName, that.imageCatalogName)
                && Objects.equals(ldapAgentVersion, that.ldapAgentVersion)
                && Objects.equals(accountId, that.accountId)
                && Objects.equals(sourceImage, that.sourceImage)
                && Objects.equals(date, that.date)
                && Objects.equals(architecture, that.architecture);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, imageName, userdata, os, osType, imageCatalogUrl, imageId,
                imageCatalogName, date, ldapAgentVersion, accountId, sourceImage, architecture);
    }
}
