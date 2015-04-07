## help: list all commands
help: Makefile
	@sed -n 's/##//p' $<

.PHONY: upload_mongo update_mongo_locally update_json
.DELETE_ON_ERROR:
.SECONDARY:

## upload_mongo: updates online mongo db from local mongo db
upload_mongo:
	mongodump --db merdb --collection questions --out ~/tmp
	mongodump --db merdb --collection topics --out ~/tmp
	mongorestore --username $(MONGOLAB_USERNAME) --password $(MONGOLAB_PASSWORD) --host $(MONGOLAB_HOST) --db $(MONGOLAB_DB) ~/tmp/merdb/ --drop

## update_mongo_locally: updates local mongo db
update_mongo_locally: import2mongo.sh
	./import2mongo.sh

## update_json: updates json files
update_json:
	echo "Please check out sister repo at https://github.com/MathEducationResources/scrape_wiki_content"
