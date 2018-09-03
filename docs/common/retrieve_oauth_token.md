### Retrieve OAuth Bearer Token via Cloudbreak REST API
In order to communicate with Cloudbreak's API, you must retrieve a bearer token. For example:

#### Using Curl
```
TOKEN=$(curl -k -iX POST -H "accept: application/x-www-form-urlencoded" -d 'credentials={"username":"admin@example.com","password":"pwd"}' "https://192.168.99.100/identity/oauth/authorize?response_type=token&client_id=cloudbreak_shell&scope.0=openid&source=login&redirect_uri=http://cloudbreak.shell" | grep location | cut -d'=' -f 3 | cut -d'&' -f 1)
```

#### Using Python
```
import requests
from six.moves.urllib import parse

username = 'admin@example.com'
password = 'pwd'
url = 'https://192.168.99.100/identity/oauth/authorize'

resp = requests.post(
    url=url,
    params={
        'response_type': 'token',
        'client_id': 'cloudbreak_shell',
        'scope.0': 'openid',
        'source': 'login',
        'redirect_uri': 'http://cloudbreak.shell'
    },
    headers={
        'accept': 'application/x-www-form-urlencoded'
    },
    verify=False,
    allow_redirects=False,
    data=[
        ('credentials',
         '{"username":"' + username + '",'
         '"password":"' + password + '"}'),
    ]
)
resp.raise_for_status()
token = parse.parse_qs(resp.next.url)['access_token'][0]
```