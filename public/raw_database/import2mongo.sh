#!/bin/bash
#import *.json in raw_database to mongo
DBNAME=merdb
COLLECTIONNAME_Q=questions
mongo $DBNAME --eval "db.questions.drop()"
LIST_Q="$(find json_data -name *.json)"
for i in $LIST_Q; do
	echo $i
	mongoimport --db $DBNAME --collection $COLLECTIONNAME_Q --file $i --jsonArray
done
mongo $DBNAME --eval "db.questions.ensureIndex({statement_html:\"text\",hints_html:\"text\",solutions_html:\"text\",answer_html:\"text\",topics:\"text\"})"
echo "Total number of QUESTIONS in the database:"
mongo $DBNAME --eval "db.questions.count()"

COLLECTIONNAME_T=topics
mongo $DBNAME --eval "db.topics.drop()"
LIST_T="$(find json_topics -name *.json)"
for i in $LIST_T; do
    echo $i
    mongoimport --db $DBNAME --collection $COLLECTIONNAME_T --file $i --jsonArray
done
mongo $DBNAME --eval "db.topics.ensureIndex({topic:\"text\"})"
echo "Total number of TOPICS in the database:"
mongo $DBNAME --eval "db.topics.count()"
