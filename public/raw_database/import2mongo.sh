#!/bin/bash
#import *.json in raw_database to mongo
DBNAME=merdb
COLLECTIONNAME=questions
LIST="$(find json_data -name *.json)"
mongo $DBNAME --eval "db.questions.drop()"
for i in $LIST; do
	echo $i
	mongoimport --db $DBNAME --collection $COLLECTIONNAME --file $i --jsonArray
done
mongo $DBNAME --eval "db.questions.ensureIndex({statement_latex:\"text\",hints_latex:\"text\",solutions_latex:\"text\",answer_latex:\"text\"})"
