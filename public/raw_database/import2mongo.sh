#!/bin/bash
#import *.json in raw_database to mongo
DBNAME=merdb
COLLECTIONNAME=questions
mongo $DBNAME --eval "db.questions.drop()"
LIST="$(find json_data -name *.json)"
for i in $LIST; do
	echo $i
	mongoimport --db $DBNAME --collection $COLLECTIONNAME --file $i --jsonArray
done
mongo $DBNAME --eval "db.questions.ensureIndex({statement_html:\"text\",hints_html:\"text\",solutions_html:\"text\",answer_html:\"text\"})"
echo "\n\nTotal number of questions in the database:\n"
mongo $DBNAME --eval "db.questions.count()"