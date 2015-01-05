## help: list all commands
help: Makefile
	@sed -n 's/##//p' $<

## all: update mongo db from scratch
all: update_json, update_mongo_locally, upload_mongo

## clean: remove all intermediate files: csv and json
clean:
	rm questions_meta.csv
	rm questions_topic.csv
	rm -rf json_data
	rm -rf test

.PHONY: all, clean, correct_image_path, update_json, update_mongo_locally, upload_mongo, upload_mongo_only
.DELETE_ON_ERROR:
.SECONDARY:

## correct image path in html version
correct_image_path:
	find json_data -type f -name *.json -print0 | xargs -0 sed -i 's:\"\.\./json_data/MATH:\"/assets/raw_database/json_data/MATH:g'

## upload_mongo_only: updates online mongo db from local mongo db WITHOUT refreshing questions locally
upload_mongo_only:
	mongodump --db merdb --collection questions --out ~/tmp
	mongorestore --username $(MERWEBAPP_MONGODB_USERNAME) --password $(MERWEBAPP_MONGODB_PASS) --host $(MERWEBAPP_MONGODB_HOST) --db $(MERWEBAPP_MONGODB_USERNAME) ~/tmp/merdb/ --drop

## upload_mongo: updates online mongo db from local mongo db
upload_mongo: update_mongo_locally
	mongodump --db merdb --collection questions --out ~/tmp
	mongorestore --username $(MERWEBAPP_MONGODB_USERNAME) --password $(MERWEBAPP_MONGODB_PASS) --host $(MERWEBAPP_MONGODB_HOST) --db $(MERWEBAPP_MONGODB_USERNAME) ~/tmp/merdb/ --drop

## upload_mongo_locally_only: updates local mongo db WITHOUT refreshing questions locally
update_mongo_locally_only: import2mongo.sh
	./import2mongo.sh

## update_mongo_locally: updates local mongo db from json files
update_mongo_locally: import2mongo.sh update_json
	./import2mongo.sh

## update_json: downloads all json files (takes a few hours)
update_json: MER2json.py questions_meta.csv questions_topic.csv
	python $< --no-latex --overwrite

## questions_meta_csv: writes meta information to csv
questions_meta.csv: MER2csv.py
	python $<

## questions_topic.csv: writes questions-topic to csv
questions_topic.csv: MER2csv.py
	python $< --topic
