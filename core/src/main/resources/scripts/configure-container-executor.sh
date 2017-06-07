#!/bin/bash -e

usermod -a -G docker cloudbreak
usermod -a -G docker yarn

echo $(date)

