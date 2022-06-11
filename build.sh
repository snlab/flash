#!/bin/bash
CMD="all"

if [ -z "$1" ]
then
    $CMD=$1
fi

install_deps() {
    echo "=== Installing dependencies ==="
    sudo apt update
    sudo apt install openjdk-17-jdk maven git python3 python3-pip
    python3 -m pip install matplotlib seaborn
}

get_datasets() {
    echo "=== Downloading datasets ==="
    git clone https://gitee.com/gdtongji/dataset
}

jar() {
    echo "=== Building artifact ==="
    mvn assembly:assembly
    cp target/flash-public-1.0-SNAPSHOT-jar-with-dependencies.jar ./flash.jar
}

case $CMD in
    "install_deps")
        install_deps()
        ;;
    "get_datsets")
        get_datasets()
        ;;
    "jar")
        jar()
        ;;
    *)
        install_deps()
        get_datasets()
        jar()