#!/bin/bash

PROJECT_PATH=`dirname $PWD`
NPMSNIFF_PATH=$PROJECT_PATH/target/npmsniff
NPMCACHE_PATH=$PROJECT_PATH/target/npmcache

# kill maven & java process
process1=`ps -ef | grep -v grep | grep maven | grep npmsim | awk '{print $2}'`
if [ "x$process1" != "x" ]; then
  echo "kill maven process $process1"
  kill $process1
fi

sleep 0.5s

process2=`ps -ef | grep -v grep | grep npmsim | awk '{print $2}'`
if [ "x$process2" != "x" ]; then
  echo "kill java process $process2"
  kill $process2
fi

# remove npm related dirs
if [ -d $NPMSNIFF_PATH ]; then
  echo "remove $NPMSNIFF_PATH"
  rm -rf $NPMSNIFF_PATH
fi
if [ -d $NPMCACHE_PATH ]; then
  echo "remove $NPMCACHE_PATH"
  rm -rf $NPMCACHE_PATH
fi

# remove mvn output
if [ -f $PROJECT_PATH/bin/mvn.out ]; then
  echo "remove $PROJECT_PATH/bin/mvn.out"
  rm $PROJECT_PATH/bin/mvn.out
fi
