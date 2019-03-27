#!/bin/bash
set -e

curl -iv -u AMBARI_USER:AMBARI_PASSWORD -H "X-Requested-By: ambari" -X POST -d '{"Users/user_name":"teszt","Users/password":"Teszt123","Users/active":true,"Users/admin":true}' http://localhost:8080/api/v1/users