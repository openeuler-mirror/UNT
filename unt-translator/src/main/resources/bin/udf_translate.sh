#!/usr/bin/env bash

binDir=$(cd $(dirname $0); pwd)

baseDir=${binDir}/..

cppDir=${baseDir}/cpp

outputDir=${baseDir}/output

confDir=${baseDir}/conf

logDir=${baseDir}/log

logFile=${baseDir}/log/bootstrap.log

jarPath=$1
engineType=$2

if [ ! -d "$logDir" ]; then
  mkdir "$logDir"
fi

if [ ! -d "$outputDir" ]; then
  mkdir "$outputDir"
fi

if [ ! -d "$cppDir" ]; then
  mkdir "$cppDir"
fi

log() {
  local timestamp=$(date "+%Y-%m-%d %H:%M:%S")
  echo "[${timestamp}] $1" | tee -a "$logFile"
}

log "start translate"

if ! python ${binDir}/locked_exec.py java -Dlog4j.configurationFile="\"${baseDir}/conf/log4j2.xml\"" -cp "\"${baseDir}/lib/*\"" com.huawei.unt.UNTMain "${jarPath}" "${engineType}" "${baseDir}"; then
  log "translate failed"
  exit 1
fi

export C_INCLUDE_PATH=${cppDir}/include:$C_INCLUDE_PATH
export CPLUS_INCLUDE_PATH=${cppDir}/include:$CPLUS_INCLUDE_PATH

sha256="$( sed -n '1p' ${baseDir}/SHA256)"

cppDir="${cppDir}/${sha256}"

outputDir="${outputDir}/${sha256}"

if [ ! -d $cppDir ]; then
  log "Translated cpp dir is not exists"
  exist 1
fi

properties_file=${confDir}/udf_tune.properties

tune_level="tune_level"

if [ ! -f "$properties_file" ]; then
    log "error: file $properties_file not exit"
    exit 1
fi

result=$(awk -F= -v key="${tune_level}" '
  BEGIN {
    exists = 0
    value = "-1"
  }
  /^[[:space:]]*[#!]/ || /^[[:space:]]*$/ { next }
  {
    gsub(/^[ \t]+|[ \t]+$/, "", $1)
    if ($1 == key) {
      val = substr($0, index($0, "=") + 1)
      gsub(/^[ \t]+|[ \t]+$/, "", val)
      gsub(/^["'\'']|["'\'']$/, "", val)
      value = tolower(val)
      exists = 1
      exit
    }

  }
  END {
    if (exists) print value
    else print "-1"
  }' "$properties_file"
)

if [ "$result" == "4" ]; then
    export CPLUS_INCLUDE_PATH=/usr/local/ksl/include:/ai4c-udf/json/include:$CPLUS_INCLUDE_PATH

    export C_INCLUDE_PATH=/usr/local/ksl/include:/ai4c-udf/json/include:$C_INCLUDE_PATH

    export LD_LIBRARY_PATH=/usr/local/ksl/lib:$LD_LIBRARY_PATH
    if which ai4c-udf > /dev/null 2>&1; then
        log "ai4c-udf start"
        ai4c-udf --input_dir="${cppDir}" --output_dir="${outputDir}" --debug=true  --c_include_dir="${C_INCLUDE_PATH}" --cxx_include_dir="${CPLUS_INCLUDE_PATH}" --ld_library_path="${LD_LIBRARY_PATH}"
        return_code=$?
        if [ $return_code -eq 0 ]; then
            log "ai4compiler success"
            exit 0;
        elif [ $return_code -eq 1 ]; then
            log "ai4compiler failed, fallback gcc"
        else
            log "ai4compiler unknown failed, fallback gcc"
        fi
    fi
fi

cd ${cppDir}

echo "Start compile translated UDFs..."

make -j
compile_return_code=$?

if [ $compile_return_code -eq 0 ]; then
    log "Translate UDFS success"
    exit 0;
elif [ $return_code -eq 1 ]; then
    log "Translate UDFS failed"
    exit 1;
else
    log "unknown error, Translate UDFS failed"
    exit 1;
fi
