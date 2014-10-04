# -*- coding: utf-8 -*-
from MER2csv import get_latex_statement_from_url, \
                    get_all_questions_from_exam, \
                    get_num_hs_question

import json
import sys
reload(sys)
sys.setdefaultencoding('utf-8')


question_urls = get_all_questions_from_exam('http://wiki.ubc.ca/Science:Math_Exam_Resources/Courses/MATH100/December_2011')

x = get_latex_statement_from_url('http://wiki.ubc.ca/Science:Math_Exam_Resources/Courses/MATH100/December_2011/Question_01_(c)', 1, 1)

print '-' * 50
print x
sys.exit()


full_db = {}
for questionURL in question_urls:
    num_hints, num_sols = get_num_hs_question(questionURL)
    try:
        full_db[questionURL] = get_latex_statement_from_url('http://wiki.ubc.ca' + questionURL, num_hints, num_sols)
    except:
        print questionURL  #todo: some pre-cleaning of special ascii characters
        print '-'*50


with open("MATH100_December2011.json", "w") as outfile:
    json.dump(full_db, outfile, indent=4)


