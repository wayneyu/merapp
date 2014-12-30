#!/bin/bash
#import *.json in raw_database to mongo
DBNAME=merdb
COLLECTIONNAME=questions
LIST="$(find json_data -name *.json)"
mongoimport --db $DBNAME --eval "db.questions.drop()"
for i in $LIST; do
	echo $i
	mongoimport --db $DBNAME --collection $COLLECTIONNAME --file $i --jsonArray
done
