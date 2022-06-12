#!/bin/bash
CMD="all"

if [ ! -z "$1" ]
then
    CMD=$1
fi

install_maven() {
    echo "=== Installing maven ==="
    mvn_version=${mvn_version:-3.8.6}
    url="https://dlcdn.apache.org/maven/maven-3/${mvn_version}/binaries/apache-maven-${mvn_version}-bin.tar.gz"
    install_dir="/opt/maven"
    echo $url

    if [ -d ${install_dir} ]; then
        mv ${install_dir} ${install_dir}.$(date +"%Y%m%d")
    fi

    mkdir ${install_dir}
    curl -fSL ${url} | tar zx --strip-components=1 -C ${install_dir}

    cat << EOF > /etc/profile.d/maven.sh
#!/bin/sh
export MAVEN_HOME=${install_dir}
export M2_HOME=${install_dir}
export M2=${install_dir}/bin
export PATH=${install_dir}/bin:$PATH
EOF

    source /etc/profile.d/maven.sh

    echo maven installed to ${install_dir}
    mvn --version
}

install_deps() {
    echo "=== Installing dependencies ==="
    sudo apt update
    sudo apt install -y openjdk-17-jdk git python3 python3-pip curl

    install_maven

    python3 -m pip install matplotlib seaborn
}

get_datasets() {
    echo "=== Downloading datasets ==="
    git clone https://bitbucket.org/gdtongji/dataset.git
}

jar() {
    echo "=== Building artifact ==="
    if [ -d /etc/profile.d/maven.sh ]; then
        install_maven
    fi
    
    mvn assembly:assembly
    cp target/flash-public-1.0-SNAPSHOT-jar-with-dependencies.jar ./flash.jar
}

case $CMD in
    "install_deps")
        install_deps
        ;;
    "get_datasets")
        get_datasets
        ;;
    "jar")
        jar
        ;;
    *)
        install_deps
        get_datasets
        jar
    ;;
esac