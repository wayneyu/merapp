# -*- coding: utf-8 -*-
from MER2csv import (get_latex_statement_from_url,
                     get_all_questions_from_exam,
                     get_num_hs_question)

import json
import sys
reload(sys)
sys.setdefaultencoding('utf-8')


question_urls = get_all_questions_from_exam(('http://wiki.ubc.ca/'
                                             'Science:Math_Exam_Resources/'
                                             'Courses/MATH100/December_2011'))

### uncomment for debugging
# x = get_latex_statement_from_url(('http://wiki.ubc.ca/'
                                  # 'Science:Math_Exam_Resources/Courses/'
                                  # 'MATH100/December_2011/Question_04_(b)'),
                                 # 1, 1)

# print("-" * 50)
# print(x['hints'][0])
# sys.exit()


full_db = {}
for questionURL in question_urls:
    num_hints, num_sols = get_num_hs_question(questionURL)
    full_db[questionURL] = get_latex_statement_from_url(('http://wiki.ubc.ca' +
                                                         questionURL),
                                                        num_hints, num_sols)

with open("MATH100_December2011.json", "w") as outfile:
    json.dump(full_db, outfile, indent=4)
