YAML_NAME=arm-images

deps:
	curl -L https://github.com/gliderlabs/sigil/releases/download/v0.4.0/sigil_0.4.0_$(shell uname)_x86_64.tgz | tar -xz -C /usr/local/bin

generate-yml:
	sigil -f $(YAML_NAME).tmpl > ./src/main/resources/$(YAML_NAME).yml
