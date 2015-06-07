Math Educational Resources
=================================

Welcome to the Math Educational Resources! This repository is served as a [webapp on heroku](http://merapp.herokuapp.com/).

[![Stories in Ready](https://badge.waffle.io/wayneyu/merapp.png?label=ready&title=Ready)](http://waffle.io/wayneyu/merapp) Task management from [waffle.io](https://waffle.io/).



Getting started
===============
##### Download typesafe reactive platform
```
https://www.typesafe.com/get-started
```

##### install mongodb 2.6 (our mongodb online repository runs 2.6, so do *NOT* install the latest version 3.x or greater)
  * Mac: `brew install homebrew/versions/mongodb26`
  * Ubuntu: [how-to from stackoverflow](http://stackoverflow.com/questions/28945921/e-unable-to-locate-package-mongodb-org)
    * #Step 1:  Import the MongoDB public key
    `sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 7F0CEB10`
    * #Step 2: Generate a file with the MongoDB repository url
    `echo 'deb http://downloads-distro.mongodb.org/repo/ubuntu-upstart dist 10gen' | sudo tee /etc/apt/sources.list.d/mongodb.list`
    * #Step 3: Refresh the local database with the packages
    `sudo apt-get update`
    * #Step 4: Install the last stable MongoDB version and all the necessary packages on our system
    `sudo apt-get install mongodb-org`

##### Get secret files from maintainer, and place it is the root folder
  * production environment setup `prod.env`
  * local environment setup `.env`

##### get local database from production

```
today=`date '+%Y_%m_%d_%H_%M_%S'`;
out="./backup_$today";
export $(cat prod.env);
mongodump -v --host $MONGOLAB_HOST --db $MONGOLAB_DB --username $MONGOLAB_USERNAME --password $MONGOLAB_PASSWORD --out ./$out
mongorestore backup_[today]/heroku_app29524184/ -d merdb
```

##### Start webapp
  * provide access to local database, etc
    * `export $(cat .env)`
    * `mongod --config /usr/local/etc/mongod.conf`
  * start webapp
    * `./activator ~run`

The webapp should now be listening at localhost (default: 9000).
