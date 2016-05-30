#!/bin/bash

{% if has_public_address %}
STATUS=$(curl -s -m 5 -w "%{http_code}" http://169.254.169.254/latest/meta-data/public-ipv4 -o /dev/null)
if [ "$STATUS" == "200" ]; then
	curl -s -m 5 http://169.254.169.254/latest/meta-data/public-ipv4
else
	dig +short myip.opendns.com @resolver1.opendns.com
fi
{% else %}
echo "{{ private_address }}"
{% endif %}
