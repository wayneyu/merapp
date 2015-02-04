#!/bin/bash
#backup mongo db
today=`date '+%Y_%m_%d_%H_%M_%S'`;
out="./backup_$today"
mongodump -v --host $MONGOLAB_HOST --db $MONGOLAB_DB --username $MONGOLAB_USERNAME --password $MONGOLAB_PASSWORD --out ./$out