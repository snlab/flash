#!/bin/bash
datasets() {
    echo "Downloading datasets..."
    git clone https://gitee.com/gdtongji/dataset
}
compile() {
    mvn assembly:assembly
}

pythonDep() {
    python -m pip install matplotlib seaborn
}
# mkdir -p $DIR
compile()
pythonDep()
cp target/flash-public-1.0-SNAPSHOT-jar-with-dependencies.jar ./flash.jar