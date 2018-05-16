#!/bin/bash

cat>amazon-osfamily.json<<EOF
{"aliases": {"amazon2017": "amazon6", "amazon2018": "amazon6"}}
EOF

for file in $(find / -name "os_family.json"); do
	jq -s '.[0] * .[1]' amazon-osfamily.json $file > merge.json
	mv -f merge.json $file
done

rm -f amazon-osfamily.json