## help: list all commands
help: Makefile
	@sed -n 's/##//p' $<

## all: update mongo db from scratch
all: update_mongo

## clean: remove all intermediate files: csv and json
clean:
	rm questions_meta.csv
	rm questions_topic.csv
	rm -rf json_data

.PHONY: all, clean
.DELETE_ON_ERROR:
.SECONDARY:

## update_mongo: updates mongo db from json files
update_mongo: import2mongo.sh update_json
	./import2mongo.sh

## update_json: downloads all json files (takes a few hours)
update_json: MER2json.py import2mongo.sh questions_meta.csv questions_topic.csv MER2json.py
	python $< --no-latex

## questions_meta_csv: writes meta information to csv
questions_meta.csv: MER2csv.py
	python $<

## questions_topic.csv: writes questions-topic to csv
questions_topic.csv: MER2csv.py
	python $< --topic
