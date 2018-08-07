####Retrieve OAuth Bearer Token via Cloudbreak REST API
In order to communicate with Cloudbreak's API, you must retrieve a bearer token. For example:
```
TOKEN=$(curl -k -iX POST -H "accept: application/x-www-form-urlencoded" -d 'credentials={"username":"admin@example.com","password":"pwd"}' "https://192.168.99.100/identity/oauth/authorize?response_type=token&client_id=cloudbreak_shell&scope.0=openid&source=login&redirect_uri=http://cloudbreak.shell" | grep location | cut -d'=' -f 3 | cut -d'&' -f 1)
```