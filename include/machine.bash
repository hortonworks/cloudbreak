
machine-init() {

  env-import MACHINE_NAME cbd
  env-import MACHINE_STORAGE_PATH $HOME/.docker/machine
  env-import MACHINE_MEM 4096
  env-import MACHINE_CPU 2
  env-import MACHINE_OPTS "--xhyve-virtio-9p"
  env-import DOCKER_PROFILE Profile
  machine-deps
}

machine-deps() {
  deps-require docker-machine 0.8.2
  deps-require docker-machine-driver-xhyve 0.3.1
  deps-require docker 1.12.3
}

machine-create() {
    declare desc="Installs docker-machine xhyve driver"
    debug "$desc"
    
    docker-machine create \
        -d xhyve \
        --xhyve-memory-size $MACHINE_MEM \
        --xhyve-cpu-count $MACHINE_CPU \
        --xhyve-boot-cmd="loglevel=3 user=docker console=ttyS0 console=tty0 noembed nomodeset norestore waitusb=10 base host=$MACHINE_NAME" \
        $MACHINE_OPTS \
        $MACHINE_NAME

    machine-env
}

machine-check() {
   declare desc="Check the Docker vm"

   debug "Check if vm is running"
   local status=$(docker-machine status $MACHINE_NAME)
   if [[ "$status" != "Running" ]]; then
       echo "docker vm is not running! status: $status"  | red
       echo "=====> start the VM:"
       echo "docker-machine start $MACHINE_NAME" | yellow
       exit 1
   fi

   debug "Check if volume sharing works"
   local localDate=$(date)
   echo $localDate > delme1.txt
   docker run -v $PWD:/data alpine cat /data/delme1.txt > delme2.txt
   if ! diff delme1.txt delme2.txt ; then
       echo "docker volume sharing doesnt work !!!" | red
   else
       echo "docker volume sharing: OK" | green 1>&2
   fi
   rm delme1.txt delme2.txt
}

machine-env() {
    declare desc="creates local profile script"
    debug "$desc"
    
    touch $DOCKER_PROFILE
    sed -i '/PUBLIC_IP/ d' Profile

    cat >> $DOCKER_PROFILE <<EOF
export PATH=$PWD/.deps/bin:\$PATH
eval \$(docker-machine env --shell bash $MACHINE_NAME)
export PUBLIC_IP=\$(docker-machine ip $MACHINE_NAME)
export DOCKER_MACHINE=$MACHINE_NAME
EOF

    debug docker ENV are saved to $DOCKER_PROFILE
    echo "=====> You can set docker ENV vars by:" 1>&2
    echo "source $DOCKER_PROFILE" | yellow
}
