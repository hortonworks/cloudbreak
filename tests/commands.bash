CB_BIN="cb"

function configure-cb() {
  DEBUG=1 $CB_BIN configure "$@"
}

function generate-cluster-template() {
  DEBUG=1 $CB_BIN cluster generate-template "$@"
}

function list-blueprints() {
  DEBUG=1 $CB_BIN blueprint list
 }

 function create-blueprint {
   DEBUG=1 $CB_BIN blueprint  create "$@"
 }

  function delete-blueprint() {
   DEBUG=1 $CB_BIN blueprint delete "$@"
 }

 function describe-blueprint() {
   DEBUG=1 $CB_BIN blueprint describe "$@"
 }

 function create-credential-aws-key() {
   DEBUG=1 $CB_BIN credential create aws key-based "$@"
 }

 function create-credential-aws-role() {
  DEBUG=1 $CB_BIN credential create aws role-based "$@"
 }

function create-credential-openstack-v2() {
 DEBUG=1 $CB_BIN credential create openstack keystone-v2 "$@"
   }

function create-credential-openstack-v3() {
  DEBUG=1 $CB_BIN credential create openstack keystone-v3 "$@"
   }

function create-credential-azure() {
  DEBUG=1 $CB_BIN credential create azure app-based "$@"
   }

function create-credential-gcp() {
 DEBUG=1 $CB_BIN credential create gcp "$@"
   }

function list-credentials() {
 DEBUG=1 $CB_BIN credential list
   }

function describe-credential() {
  DEBUG=1 $CB_BIN credential describe "$@"
   }
function delete-credential() {
  DEBUG=1 $CB_BIN credential delete "$@"
   }

function create-recipe(){
  DEBUG=1 $CB_BIN recipe create "$@"
}

function list-recipes() {
 DEBUG=1 $CB_BIN  recipe list
}

function delete-recipe() {
 DEBUG=1 $CB_BIN recipe delete "$@"
}

function describe-recipe() {
 DEBUG=1 $CB_BIN recipe describe "$@"
}

function create-cluster() {
 DEBUG=1 $CB_BIN cluster create "$@"
}

function start-cluster() {
 DEBUG=1 $CB_BIN cluster start "$@"
}

function stop-cluster() {
 DEBUG=1 $CB_BIN cluster stop "$@"
}

function list-clusters() {
 DEBUG=1  $CB_BIN cluster list
}

function delete-cluster() {
 DEBUG=1 $CB_BIN cluster delete "$@"
}

function describe-cluster() {
 DEBUG=1 $CB_BIN cluster describe "$@"
}

function scale-cluster() {
 DEBUG=1 $CB_BIN  cluster scale "$@"
}

function repair-cluster() {
 DEBUG=1 $CB_BIN  cluster repair "$@"
}

function sync-cluster() {
 DEBUG=1 $CB_BIN cluster sync "$@"
}

function change-ambari-password() {
 DEBUG=1 $CB_BIN  cluster change-ambari-password "$@"
}

function reinstall-cluster() {
 DEBUG=1 $CB_BIN cluster reinstall "$@"
}

function generate-reinstall-template() {
 DEBUG=1 $CB_BIN cluster generate-reinstall-template "$@"
}

function list-ldaps() {
 DEBUG=1 $CB_BIN  ldap list "$@"
}

function create-ldap() {
 DEBUG=1 $CB_BIN  ldap create "$@"
}

function delete-ldap() {
 DEBUG=1 $CB_BIN  ldap delete "$@"
}

function availability-zone-list() {
 DEBUG=1 $CB_BIN  cloud availability-zones "$@"
}

function region-list() {
 DEBUG=1 $CB_BIN  cloud regions "$@"
}

function instance-list() {
 DEBUG=1 $CB_BIN  cloud instances "$@"
}

function volume-list() {
 DEBUG=1 $CB_BIN  cloud volumes "$@"
}

function list-image-catalog() {
 DEBUG=1 $CB_BIN  imagecatalog list "$@"
}

function create-image-catalog() {
 DEBUG=1 $CB_BIN  imagecatalog create "$@"
}

function get-images() {
 DEBUG=1 $CB_BIN  imagecatalog images "$@"
}

function delete-image-catalog() {
 DEBUG=1 $CB_BIN  imagecatalog delete "$@"
}

function set-default-image-catalog() {
 DEBUG=1 $CB_BIN  imagecatalog set-default "$@"
}