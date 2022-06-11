#!/bin/bash
# DIR=sigcomm22-eval
# if [ $# -eq 1 ]
# then
#     DIR=$1
# fi
compile() {
    mvn package
}

pythonDep() {
    python -m pip install matplotlib seaborn
}
# mkdir -p $DIR
compile()
# cp target/flash-public.jar $DIR/flash.jar