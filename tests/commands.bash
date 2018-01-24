CB_BIN="DEBUG=1 cb"

function configure-cb() {
  "$CB_BIN" configure "$@"
}

function generate-cluster-template() {
  "$CB_BIN" cluster generate-template "$@"
}

function list-blueprints() {
  "$CB_BIN" blueprint list
 }

 function create-blueprint {
   "$CB_BIN" blueprint  create "$@"
 }

  function delete-blueprint() {
   "$CB_BIN" blueprint delete "$@"
 }

 function describe-blueprint() {
   "$CB_BIN" blueprint describe "$@"
 }

 function create-credential-aws-key() {
   "$CB_BIN" credential create aws key-based "$@"
 }

 function create-credential-aws-role() {
  "$CB_BIN" credential create aws role-based "$@"
 }

function create-credential-openstack-v2() {
 "$CB_BIN" credential create openstack keystone-v2 "$@"
   }

function create-credential-openstack-v3() {
  "$CB_BIN" credential create openstack keystone-v3 "$@"
   }

function create-credential-azure() {
  "$CB_BIN" credential create azure app-based "$@"
   }

function create-credential-gcp() {
 "$CB_BIN" credential create gcp "$@"
   }

function list-credentials() {
 "$CB_BIN" credential list
   }

function describe-credential() {
  "$CB_BIN" credential describe "$@"
   }
function delete-credential() {
  "$CB_BIN" credential delete "$@"
   }

function create-recipe(){
  "$CB_BIN" recipe create "$@"
}

function list-recipes() {
 "$CB_BIN" recipe list
}

function delete-recipe() {
 "$CB_BIN" recipe delete "$@"
}

function describe-recipe() {
 "$CB_BIN" recipe describe "$@"
}

function create-cluster() {
 "$CB_BIN" cluster create "$@"
}

function start-cluster() {
 "$CB_BIN" cluster start "$@"
}

function stop-cluster() {
 "$CB_BIN" cluster stop "$@"
}

function list-clusters() {
 "$CB_BIN" cluster list
}

function delete-cluster() {
 "$CB_BIN" cluster delete "$@"
}

function describe-cluster() {
 "$CB_BIN" cluster describe "$@"
}

function scale-cluster() {
 "$CB_BIN" cluster scale "$@"
}

function repair-cluster() {
 "$CB_BIN" cluster repair "$@"
}

function sync-cluster() {
 "$CB_BIN" cluster sync "$@"
}

function change-ambari-password() {
 "$CB_BIN" cluster change-ambari-password "$@"
}

function reinstall-cluster() {
 "$CB_BIN" cluster reinstall "$@"
}

function generate-reinstall-template() {
 "$CB_BIN" cluster generate-reinstall-template "$@"
}

function list-ldaps() {
 "$CB_BIN" ldap list "$@"
}

function create-ldap() {
 "$CB_BIN" ldap create "$@"
}

function delete-ldap() {
 "$CB_BIN"  ldap delete "$@"
}

function availability-zone-list() {
 "$CB_BIN" cloud availability-zones "$@"
}

function region-list() {
 "$CB_BIN" cloud regions "$@"
}

function instance-list() {
 "$CB_BIN" cloud instances "$@"
}

function volume-list() {
 "$CB_BIN" cloud volumes "$@"
}

function list-image-catalog() {
 "$CB_BIN" imagecatalog list "$@"
}

function create-image-catalog() {
 "$CB_BIN" imagecatalog create "$@"
}

function get-images() {
 "$CB_BIN" imagecatalog images "$@"
}

function delete-image-catalog() {
 "$CB_BIN" imagecatalog delete "$@"
}

function set-default-image-catalog() {
 "$CB_BIN" imagecatalog set-default "$@"
}