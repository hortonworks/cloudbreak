echo Hello world!

date
curl 'https://127.0.0.1/cb/api/v1/blueprints/account' -H 'accept: application/json, text/plain, */*' --compressed --insecure

curl -sX POST --insecure  -H "Accept: application/x-www-form-urlencoded" --data-urlencode 'credentials={"username":"admin@example.com","password":"cloudbreak"}' "https://127.0.0.1/identity/oauth/authorize?response_type=token&client_id=cloudbreak_shell" -w '%{redirect_url}'

export CLOUD_URL=https://127.0.0.1
export EMAIL=admin@example.com
export PASSWORD=cloudbreak
#could be a bug:
cb configure --server $CLOUD_URL --username $EMAIL --password $PASSWORD
bats *.bats
