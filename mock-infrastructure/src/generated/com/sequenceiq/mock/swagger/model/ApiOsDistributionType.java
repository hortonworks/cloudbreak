package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Allowed OS distribution types.
 */
public enum ApiOsDistributionType {
  
  UNKNOWN("UNKNOWN"),
  
  RHEL5("RHEL5"),
  
  RHEL6("RHEL6"),
  
  RHEL7("RHEL7"),
  
  RHEL8("RHEL8"),
  
  SLES11("SLES11"),
  
  SLES12("SLES12"),
  
  DEBIAN_SQUEEZE("DEBIAN_SQUEEZE"),
  
  DEBIAN_WHEEZY("DEBIAN_WHEEZY"),
  
  DEBIAN_JESSIE("DEBIAN_JESSIE"),
  
  UBUNTU_LUCID("UBUNTU_LUCID"),
  
  UBUNTU_MAVERICK("UBUNTU_MAVERICK"),
  
  UBUNTU_PRECISE("UBUNTU_PRECISE"),
  
  UBUNTU_TRUSTY("UBUNTU_TRUSTY"),
  
  UBUNTU_XENIAL("UBUNTU_XENIAL"),
  
  UBUNTU_BIONIC("UBUNTU_BIONIC"),
  
  UBUNTU_FOCAL("UBUNTU_FOCAL"),
  
  RHEL7_PPC64LE("RHEL7_PPC64LE"),
  
  RHEL8_PPC64LE("RHEL8_PPC64LE");

  private String value;

  ApiOsDistributionType(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ApiOsDistributionType fromValue(String text) {
    for (ApiOsDistributionType b : ApiOsDistributionType.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

