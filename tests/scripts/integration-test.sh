#!/usr/bin/env sh

echo Hello world!

date
curl 'https://192.168.99.100/cb/api/v1/blueprints/account' -H 'accept: application/json, text/plain, */*' --compressed --insecure

curl -sX POST --insecure  -H "Accept: application/x-www-form-urlencoded" --data-urlencode 'credentials={"username":"admin@example.com","password":"cloudbreak"}' "https://127.0.0.1/identity/oauth/authorize?response_type=token&client_id=cloudbreak_shell" -w '%{redirect_url}'

export BASE_URL=https://192.168.99.100
export USERNAME_CLI=admin@example.com
export PASSWORD_CLI=cloudbreak
#could be a bug:
cb configure --server $BASE_URL --username $USERNAME_CLI --password $PASSWORD_CLI
bats integration/*.bats
