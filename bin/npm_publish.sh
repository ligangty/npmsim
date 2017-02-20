#!/bin/bash

PROJECT_PATH=`dirname $PWD`
NPMSNIFF_PATH=$PROJECT_PATH/target/npmsniff

cd $NPMSNIFF_PATH
npm publish --verbose
