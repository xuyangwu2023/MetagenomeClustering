. `dirname $0`/load_config.sh
INPUT=${OUTPUT_PREFIX}/${PREFIX}_kmerreads.txt_$K
OUTPUT=$OUTPUT_PREFIX/${PREFIX}_edges.txt_$K
WAIT=1

CMD=`cat<<EOF
$SPARK_SUBMIT --master $MASTER --deploy-mode client --driver-memory 55G --driver-cores 5 --executor-memory 18G --executor-cores 2 --conf spark.executor.extraClassPath=$TARGET --conf spark.driver.maxResultSize=5g --conf spark.network.timeout=360000 --conf spark.speculation=true  --conf spark.default.parallelism=$PL --conf spark.eventLog.enabled=$ENABLE_LOG  $TARGET \
GraphGen2 --wait $WAIT -i $INPUT -o $OUTPUT  --min_shared_kmers $min_shared_kmers --max_degree $max_degree
EOF`

echo $CMD

if [ $# -gt 0 ]
  then
     nohup $CMD 
     echo "submitted"
else
     echo "dry-run, not runing"
fi
