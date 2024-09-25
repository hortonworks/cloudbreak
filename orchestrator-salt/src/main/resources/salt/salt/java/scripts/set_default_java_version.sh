#!/usr/bin/env bash

set -e

CURRENT_JAVA_VERSION=$(java -version 2>&1 | grep -oP "version [^0-9]?(1\.)?\K\d+" || true)

if [ -z "$CURRENT_JAVA_VERSION" ]; then
  echo "Failed to determine the current java version."
  exit 1;
elif [ $CURRENT_JAVA_VERSION -eq $TARGET_JAVA_VERSION ]; then
  echo "Current java version equals to the target version."
  exit 0;
elif [ -n "$TARGET_JAVA_VERSION" ]; then
  echo "Change java version to $TARGET_JAVA_VERSION"
  ALTERNATIVE_TARGET=$TARGET_JAVA_VERSION
  if [ "$TARGET_JAVA_VERSION" -eq "8" ]; then
    ALTERNATIVE_TARGET=1.8.0
  fi
  if [[ $CPU_ARCH =~ ^aarch64 ]]; then
    alternatives --set java "java-$ALTERNATIVE_TARGET-openjdk.aarch64"
  else
    alternatives --set java "java-$ALTERNATIVE_TARGET-openjdk.x86_64"
  fi
  ln -sfn "/etc/alternatives/java_sdk_$ALTERNATIVE_TARGET" "/usr/lib/jvm/java"
  mkdir -p "/etc/alternatives/java_sdk_$ALTERNATIVE_TARGET/jre/lib/security"
  if [ "$TARGET_JAVA_VERSION" -ne "8" ]; then
    ln -sfn "/etc/alternatives/java_sdk_$ALTERNATIVE_TARGET/conf/security/java.security" "/etc/alternatives/java_sdk_$ALTERNATIVE_TARGET/jre/lib/security/java.security"
  fi
  ln -sfn "/etc/pki/java/cacerts" "/etc/alternatives/java_sdk_$ALTERNATIVE_TARGET/jre/lib/security/cacerts"

  #Finally validate the current java version is the target one
  CURRENT_JAVA_VERSION=$(java -version 2>&1 | grep -oP "version [^0-9]?(1\.)?\K\d+" || true)
  if [ "$CURRENT_JAVA_VERSION" -eq "$TARGET_JAVA_VERSION" ]; then
    echo "Java version successfully changed to $TARGET_JAVA_VERSION."
  else
    echo "Failed to change java version to $TARGET_JAVA_VERSION. Java version is $CURRENT_JAVA_VERSION"
    exit 1
  fi
fi
