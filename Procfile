#web: target/universal/stage/bin/myapp -Dhttp.port=${PORT} -DapplyEvolutions.default=true -Ddb.default.driver=org.postgresql.Driver -Ddb.default.url=${DATABASE_URL}
web: target/universal/stage/bin/mer -Dhttp.port=$PORT -Dmongodb.uri=$MONGOLAB_URI -Dredis.uri=$REDISCLOUD_URL
#web: target/start -Dhttp.port=$PORT -Dmongodb.uri=$MONGOLAB_URL