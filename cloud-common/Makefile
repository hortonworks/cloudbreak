deps:
	curl -L https://github.com/gliderlabs/sigil/releases/download/v0.4.0/sigil_0.4.0_$(shell uname)_x86_64.tgz | tar -xz -C /usr/local/bin


AMBARI_VERSION = $(shell sigil -f ambari-version.tmpl cloud=amazon)

update-ambari-image:
	sed -i "/hortonworks\/ambari/ s/:[^:]*$$/:$(AMBARI_VERSION)/" src/main/resources/application.yml
