#!/bin/bash
: <<USAGE
========================================================
this script is intended to be run in a docker container
========================================================
docker run -it --rm \
  --net=container:environment
  --entrypoint /bin/bash \
  hortonworks/environment -c /wait_for_environment_api.sh
USAGE


url="http://127.0.0.1:8080/env/health"
maxAttempts=10
pollTimeout=30

cat <<EOF
========================================================
= echo this container waits for environment availabilty =
= by checking the health url:
=   $url
=
= maxAttempts=$maxAttempts
========================================================
EOF

for (( i=1; i<=$maxAttempts; i++ ))
do
    echo "GET $url. Attempt #$i"
    code=`curl -sL -w "%{http_code}\\n" "$url" -o /dev/null`
    echo "Found code $code"
    if [ "x$code" = "x200" ]
    then
         echo "Hortonworks Environment is available!"
         break
    elif [ $i -eq $maxAttempts ]
    then
         echo "Cloudera Environment not started in time."
         exit 1
    fi
    sleep $pollTimeout
done