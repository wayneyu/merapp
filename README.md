Math Educational Resources
=================================

Welcome to the Math Educational Resources! This repository is served on the our [webapp on heroku](http://merapp.herokuapp.com/).

[![Stories in Ready](https://badge.waffle.io/wayneyu/merapp.png?label=ready&title=Ready)](http://waffle.io/wayneyu/merapp) Task management from [waffle.io](https://waffle.io/).



Getting started
===============
(MacOS)
Download typesafe reactive platform
`https://www.typesafe.com/get-started`

install mongodb 2.6
`brew install homebrew/versions/mongodb26`

get secret files from maintainer, and place it is the root folder
production environment setup
`prod.env`
local environment setup
`.env`

to start
`export $(cat prod.env)`
`./activator ~run`

to start with local db


get db from production
today=`date '+%Y_%m_%d_%H_%M_%S'`; out="./backup_$today"; export $(cat prod.env); mongodump -v --host $MONGOLAB_HOST --db $MONGOLAB_DB --username $MONGOLAB_USERNAME --password $MONGOLAB_PASSWORD --out ./$out

import into local db
`mongod --config /usr/local/etc/mongod.conf`
`pw -aux | grep mongod`
mongorestore backup_[today]/heroku_app29524184/ -d merdb

`export $(cat .env)`
`./activator ~run`
