
cloudbreak-init() {
  cloudbreak-tags
}

cloudbreak-tags() {
    declare desc="Sets docker image tags"

   
    env-import DOCKER_TAG_ALPINE 3.1
    env-import DOCKER_TAG_CONSUL v0.5.0-v3
    env-import DOCKER_TAG_REGISTRATOR v5
    env-import DOCKER_TAG_POSTGRES 9.4.0
    env-import DOCKER_TAG_UAA 1.8.1-v1
    env-import DOCKER_TAG_CBSHELL 0.2.47
    env-import DOCKER_TAG_CLOUDBREAK 0.3.92
    env-import DOCKER_TAG_ULUWATU 0.1.415
    env-import DOCKER_TAG_SULTANS 0.1.61
    env-import DOCKER_TAG_PERISCOPE 0.1.36
}

