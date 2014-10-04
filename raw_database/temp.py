from MER2csv import get_latex_statement_from_url, \
                    get_all_questions_from_exam, \
                    get_num_hs_question

import json

question_urls = get_all_questions_from_exam('http://wiki.ubc.ca/Science:Math_Exam_Resources/Courses/MATH100/December_2011')

full_db = {}
for questionURL in question_urls:
    num_hints, num_sols = get_num_hs_question(questionURL)
    try:
        full_db[questionURL] = get_latex_statement_from_url('http://wiki.ubc.ca' + questionURL, num_hints, num_sols)
    except:
        print questionURL  #todo: some pre-cleaning of special ascii characters


with open("MATH100_December2011.json", "w") as outfile:
    json.dump(full_db, outfile, indent=4)


