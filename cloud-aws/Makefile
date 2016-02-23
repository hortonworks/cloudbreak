deps:
	curl -L https://github.com/gliderlabs/sigil/releases/download/v0.4.0/sigil_0.4.0_$(shell uname)_x86_64.tgz | tar -xz -C /usr/local/bin

generate-yml:
	sigil -f aws-images.tmpl > ./src/main/resources/aws-images.yml
