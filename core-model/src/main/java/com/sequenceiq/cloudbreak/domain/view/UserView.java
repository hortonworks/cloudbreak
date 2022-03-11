package com.sequenceiq.cloudbreak.domain.view;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Entity
@Table(name = "users")
// It's only here, because of findbugs does not know the fields will be set by JPA with Reflection
@SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
public class UserView implements ProvisionEntity {

    @Id
    private Long id;

    @Column(name = "username")
    private String userName;

    @Column(name = "userid")
    private String userId;

    @Column(name = "usercrn")
    private String userCrn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserCrn() {
        return userCrn;
    }

    public void setUserCrn(String userCrn) {
        this.userCrn = userCrn;
    }

    @Override
    public String toString() {
        return "UserView{" +
                "id=" + id +
                ", userName='" + userName + '\'' +
                ", userId='" + userId + '\'' +
                ", userCrn='" + userCrn + '\'' +
                '}';
    }
}
