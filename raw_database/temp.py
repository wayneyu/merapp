# -*- coding: utf-8 -*-
from MER2csv import (get_latex_statement_from_url,
                     get_all_questions_from_exam,
                     get_num_hs_question)

import json
import os
import sys
reload(sys)
sys.setdefaultencoding('utf-8')

course = "MATH100"
exam = "December_2013"
directory = os.path.join('json_data', course, exam)

#import urllib
# urllib.urlretrieve(
#    "http://wiki.ubc.ca/images/6/6b/Math100_December_2012_Problem_8_Solution.jpg",
#    "test.jpg")

# images are at http://wiki.ubc.ca/File:Math100_December2012_Q7.png
# on that page, find the link to the raw graphic


def get_course_year_term_question_from_url(url):
    course, term_year, question = url.split('/')[3:6]
    print(course, term_year, question)
    term, year = term_year.split('_')

    return course, int(year), term, question

question_urls = get_all_questions_from_exam(('http://wiki.ubc.ca/'
                                             'Science:Math_Exam_Resources/'
                                             'Courses/' + course + '/' + exam))

# uncomment for debugging
# x = get_latex_statement_from_url(('http://wiki.ubc.ca/'
# 'Science:Math_Exam_Resources/Courses/'
# 'MATH100/December_2011/Question_04_(b)'),
# 1, 1)

# print("-" * 50)
# print(x['hints'][0])
# sys.exit()


for questionURL in question_urls:
    num_hints, num_sols = get_num_hs_question(questionURL)
    question_latex = get_latex_statement_from_url(('http://wiki.ubc.ca' +
                                                   questionURL),
                                                  num_hints, num_sols)
    course, year, term, \
        question = get_course_year_term_question_from_url(questionURL)
    question_json = {"course": course,
                     "year": year,
                     "term": term,
                     "statement": question_latex['statement'],
                     "hints": question_latex['hints'],
                     "sols": question_latex['sols']}

    if not os.path.exists(directory):
        os.makedirs(directory)

    with open(os.path.join(directory, question + ".json"), "w") as outfile:
        json.dump(question_json, outfile, indent=4)
