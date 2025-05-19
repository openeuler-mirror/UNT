#!/usr/bin/env bash

binDir=$(cd $(dirname $0); pwd)

baseDir=${binDir}/..

cppDir=${baseDir}/cpp

outputDir=${baseDir}/output

jarPath=$1
engineType=$2

if [ ! -d "$outputDir" ]; then
  mkdir "$outputDir"
fi

if [ ! -d "$cppDir" ]; then
  mkdir "$cppDir"
fi

java -Dlog4j.configurationFile="${baseDir}/conf/log4j2.xml" -cp "${baseDir}/lib/*" com.huawei.unt.UNTMain "${jarPath}" "${engineType}" "${baseDir}"

export C_INCLUDE_PATH=${cppDir}/include:$C_INCLUDE_PATH
export CPLUS_INCLUDE_PATH=${cppDir}/include:$CPLUS_INCLUDE_PATH

sha256="$( sed -n '1p' ${baseDir}/SHA256)"

cppDir="${cppDir}/${sha256}"

if [ ! -d $cppDir ]; then
  echo "Translated cpp dir is not exists"
  exist 1
fi

cd ${cppDir}

echo "Start compile translated UDFs..."

make -j

echo "Translate UDFs success."
