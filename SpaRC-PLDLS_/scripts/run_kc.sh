
. `dirname $0`/load_config.sh

OUTPUT=$OUTPUT_PREFIX/${PREFIX}_kc_seq_$K
WAIT=1
OPTS=-C

#执行KmerCounting
CMD=`cat <<EOF 
$SPARK_SUBMIT --master $MASTER --deploy-mode client --driver-memory 55G --driver-cores 5 --executor-memory 18G  --executor-cores 2   --conf spark.network.timeout=360000 --conf spark.default.parallelism=$PL --conf spark.executor.extraClassPath=$TARGET --conf spark.speculation=true --conf spark.speculation.multiplier=2 --conf spark.eventLog.enabled=$ENABLE_LOG $TARGET \
KmerCounting --wait $WAIT  -i $INPUT  -o $OUTPUT  --format seq -k $K $OPTS  
EOF
`
echo $CMD

if [ $# -gt 0 ]
  then
     nohup $CMD  ##nohup 英文全称 no hang up（不挂起），在系统后台不挂断地运行命令，退出终端不会影响程序的运行。
     echo "submitted"
else
     echo "dry-run, not runing"
fi

