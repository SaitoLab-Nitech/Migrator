#!/bin/bash

if [ $# -ne 1 ]; then
  echo 'Usage: ./setup.sh $ANDROID_HOME'
  exit 1
fi

AHOME=$1
FILES=`find frameworks build -type f`

for f in $FILES
do
  cp $AHOME/$f{,.bak}
  cp $f $AHOME/$f
done

cp -r ./packages/apps/MigrationManager $AHOME/packages/apps/

echo "Setup has done!"
echo "Next, you should build your Android!"
