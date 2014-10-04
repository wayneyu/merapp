#!/usr/bin/python

import MySQLdb

# Open database connection
db = MySQLdb.connect(
    "sql2.freesqldatabase.com", "sql250911", "dV3!bZ1!", "sql250911")

# prepare a cursor object using cursor() method
cursor = db.cursor()
f = open('raw_data.csv', 'r')
log_errors = open('log_errors.txt', 'a')

# execute SQL query using execute() method.
cursor.execute("""DROP TABLE IF EXISTS Questions""")
cursor.execute("""CREATE TABLE IF NOT EXISTS Questions
(url VARCHAR(255) PRIMARY KEY,
course TEXT,
exam TEXT,
question TEXT,
num_votes INT,
rating INT,
num_hints INT,
num_sols INT
)""")

for line in f:
    url, course, exam, question, num_votes, rating, num_hints, num_sols = line.split(
        ",")
    url = "{" + url + "}"
    question = "{" + question + "}"
    num_votes = int(num_votes)
    try:
        rating = int(rating)
    except ValueError:
        rating = 'NULL'
    num_hints = int(num_hints)
    num_votes = int(num_votes)
    try:
        cursor.execute(
            """INSERT INTO Questions VALUES ('%s', '%s', '%s', '%s', %s, %s, %s, %s)""" %
            (url, course, exam, question, num_votes, rating, num_hints, num_sols))
    except MySQLdb.IntegrityError, e:
        log_errors.write(url + '\n')

log_errors.close()
f.close()
# disconnect from server
db.commit()
db.close()
