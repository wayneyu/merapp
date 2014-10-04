# -*- coding: utf-8 -*-
import urllib
import lxml
import lxml.html
import os
import pypandoc
import re
import sys
reload(sys)
sys.setdefaultencoding('utf-8')

def mediawiki_from_edit(input):
    return input.split('name="wpTextbox1">')[1].split('</textarea')[0]

def preCleaning(input):
    print input
    input = input.decode('latin1')
    input = input.replace('&lt;', '<')
    input = input.replace('&amp;', '&')
    input = input.replace(u'221e', '<math>\infty</math>')
#    input = input.replace('’', "'")
    input = input.replace(u'â', '<math>\infty</math>')
#    input = input.replace('≤', '<math>\leq</math>')
#    input = input.replace('≥', '<math>\geq</math>')
#    input = input.replace('°', '<math>^{\circ}</math>')
    input = re.sub(r': +<math>', ':<math>', input)
    print input

    return input

def get_latex_statement_from_url(questionURL, num_hints=1, num_sols=1):
    def get_dict_action_urls(action):
        statementURL = questionURL.replace("Science", "index.php?title=Science") + "/Statement&action=" + action
        hintURL = questionURL.replace("Science", "index.php?title=Science") + "/Hint_"
        solURL = questionURL.replace("Science", "index.php?title=Science") + "/Solution_"

        return {'statementURL': statementURL,
            'hintsURLs': [hintURL + str(num+1) + "&action=" + action for num in range(num_hints)],
            'solsURLs': [solURL + str(num+1) + "&action=" + action for num in range(num_sols)]}

    urls = get_dict_action_urls(action='edit')

    def edit_to_latex(shs='statementURL', index=0):
        try:
            out = urllib.urlopen(urls[shs]).read()
        except AttributeError:
            out = urllib.urlopen(urls[shs][index]).read()
        out = mediawiki_from_edit(out)
        #try:
        return pypandoc.convert(preCleaning(out), 'latex', format='mediawiki')
        #except:
        #    return "NULL"


    return {'statement': [edit_to_latex('statementURL')],
    'hints': [edit_to_latex('hintsURLs', index) for index in range(len(urls['hintsURLs']))],
    'sols': [edit_to_latex('solsURLs', index) for index in range(len(urls['solsURLs']))]}




def get_all_courses(MER_URL):
    connection = urllib.urlopen(MER_URL)
    dom =  lxml.html.fromstring(connection.read())
    searchText = '/Science:Math_Exam_Resources/Courses/MATH'
    courseLinks = []
    for link in dom.xpath('//a/@href'): # select the url in href for all a tags(links)
        if searchText in link:
            courseLinks.append(link)
    courseLinks = list(set(courseLinks))
    courseLinks.sort()
    return courseLinks


def get_all_exams_from_course(courseURL):
    connection = urllib.urlopen(courseURL)
    dom =  lxml.html.fromstring(connection.read())
    searchTextA = courseURL.split(':')[2] + '/April'
    searchTextD = courseURL.split(':')[2] + '/December'
    examLinks = []
    for link in dom.xpath('//a/@href'): # select the url in href for all a tags(links)
        if searchTextA in link or searchTextD in link:
            examLinks.append(link)
    return examLinks


def get_all_questions_from_exam(examURL):
    connection = urllib.urlopen(examURL)
    dom =  lxml.html.fromstring(connection.read())
    searchText = examURL.split(':')[2] + '/Question'
    questionLinks = []
    for link in dom.xpath('//a/@href'): # select the url in href for all a tags(links)
        if searchText in link:
            questionLinks.append(link)
    return questionLinks


def get_content_rating_numvotes(questionURL):
    requestURL = questionURL.replace('/Science','http://wiki.ubc.ca/Science')
    raw = urllib.urlopen(requestURL).read()
    ratingNumvotes = raw.split('<span id="w4g_rb_area-1">Current user rating: <b>')
    if len(ratingNumvotes) == 2:
        ratingNumvotes = ratingNumvotes[1].split('</span>')[0]
        rating = int(ratingNumvotes.split('/100')[0])
        numvotes = int(ratingNumvotes.split('(')[1].split(' ')[0])
    else:
        rating = None
        numvotes = 0           
    return rating,numvotes


def get_num_hs_question(questionURL):
    num_hints = 1
    tryer = True
    while tryer:
        requestURL = 'http://wiki.ubc.ca' + questionURL + '/Hint_' + str(num_hints)
        raw = urllib.urlopen(requestURL).read()
        if 'There is currently no text in this page' in raw:
            tryer = False
            num_hints = num_hints-1
        else:
            num_hints = num_hints+1     
    num_sols = 1
    tryer = True
    while tryer:
        requestURL = 'http://wiki.ubc.ca' + questionURL + '/Solution_' + str(num_sols)
        raw = urllib.urlopen(requestURL).read()
        if 'There is currently no text in this page' in raw:
            tryer = False
            num_sols = num_sols-1
        else:
            num_sols = num_sols+1
    return num_hints,num_sols



def create_lists_for_examURLs(examURL):
    URLs = []
    courses = []
    exams = []
    questions = []
    num_votes = []
    ratings = []
    num_hints = []
    num_sols = []
    
    questionURLs = get_all_questions_from_exam(examURL)
    for questionURL in questionURLs:
        question_info = questionURL.split('/')
        
        URLs.append('http://wiki.ubc.ca' + questionURL)
        courses.append(question_info[3])
        exams.append(question_info[4])
        
        question = question_info[5]
        question = question.replace('Question_0','')
        question = question.replace('Question_','')
        question = question.replace('_',' ')
        questions.append(question)
        
        rating,numvote = get_content_rating_numvotes(questionURL)
        ratings.append(rating)
        num_votes.append(numvote)
        
        num_hint,num_sol = get_num_hs_question(questionURL)
        num_hints.append(num_hint)
        num_sols.append(num_sol)
    return URLs,courses,exams,questions,num_votes,ratings,num_hints,num_sols


def create_lists_for_courseURLs(courseURL):
    examURLs = get_all_exams_from_course(courseURL)
    URLs = []
    courses = []
    exams = []
    questions = []
    num_votes = []
    ratings = []
    num_hints = []
    num_sols = []
    for examURL in examURLs:
        URL,course,exam,question,num_vote,rating,num_hint,num_sol = create_lists_for_examURLs('http://wiki.ubc.ca' + examURL)
        URLs.extend(URL)
        courses.extend(course)
        exams.extend(exam)
        questions.extend(question)
        num_votes.extend(num_vote)
        ratings.extend(rating)
        num_hints.extend(num_hint)
        num_sols.extend(num_sol)
    return URLs,courses,exams,questions,num_votes,ratings,num_hints,num_sols   


def create_lists_for_SQL():
    MER_URL = 'http://wiki.ubc.ca/Science:Math_Exam_Resources'
    courseURLs = get_all_courses(MER_URL)
    URLs = []
    courses = []
    exams = []
    questions = []
    num_votes = []
    ratings = []
    num_hints = []
    num_sols = []
    for courseURL in courseURLs:
        URL,course,exam,question,num_vote,rating,num_hint,num_sol = create_lists_for_courseURLs('http://wiki.ubc.ca' + courseURL)
        URLs.extend(URL)
        courses.extend(course)
        exams.extend(exam)
        questions.extend(question)
        num_votes.extend(num_vote)
        ratings.extend(rating)
        num_hints.extend(num_hint)
        num_sols.extend(num_sol)
    return URLs,courses,exams,questions,num_votes,ratings,num_hints,num_sols 


def main():
    URLs,courses,exams,questions,num_votes,ratings,num_hints,num_sols = create_lists_for_SQL()
    f = file("raw_data.csv", 'w')
    for u,c,e,q,v,r,h,s in zip(URLs,courses,exams,questions,num_votes,ratings,num_hints,num_sols):
        f.write("%s,%s,%s,%s,%s,%s,%s,%s\n" %(u,c,e,q,v,r,h,s))
    f.close()

if __name__ == "__main__":
    main()
