#!/bin/bash

PROJECT_PATH=`dirname $PWD`
NPMSNIFF_PATH=$PROJECT_PATH/target/npmsniff
NPMCACHE_PATH=$PROJECT_PATH/target/npmcache
PORT=8000

if [ "x$1" == "x-p" ] || [ "x$1" == "x--port" ]; then
    PORT=$2
fi

echo "starting simulation server, see $PROJECT_PATH/bin/mvn.out for details"
nohup mvn -f $PROJECT_PATH/pom.xml clean install exec:exec -DstartPort=$PORT > $PROJECT_PATH/bin/mvn.out 2>&1 &
sleep 5s
echo "simulation server started"

echo "create test npmsniff project"
mkdir -p $NPMSNIFF_PATH
git clone https://github.com/ligangty/npmsniff.git $NPMSNIFF_PATH
mkdir -p $NPMCACHE_PATH
touch $NPMSNIFF_PATH/.npmrc
cat > $NPMSNIFF_PATH/.npmrc <<-EOF
strict-ssl=true
scope=
//localhost:$PORT/:_authToken=8d0afba2-0819-56fa-9834-2311c895faaf
registry=http://localhost:$PORT
cache=$NPMCACHE_PATH
EOF
echo "test npmsniff project created in $NPMSNIFF_PATH"
