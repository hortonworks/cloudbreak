#!/usr/bin/env bash

ldapsearch -x -h localhost -b cn=config '(objectclass=nsds5replicationagreement)' | grep '^objectClass: nsds5replicationagreement'