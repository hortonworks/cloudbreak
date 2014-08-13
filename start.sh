#/bin/bash

: ${CB_API_URL:? required parameter please check $CB_API_URL/health }

cat > /usr/local/nginx/html/connection.properties <<EOF
{
    "backend_url": "$CB_API_URL"
}
EOF

nginx
