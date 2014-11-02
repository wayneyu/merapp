# -*- coding: utf-8 -*-
import urllib
import lxml
import lxml.html
import pypandoc
import re
import warnings
import os
import subprocess
import json
import sys
reload(sys)
sys.setdefaultencoding('utf-8')


def handleImages(content, directory):

    def save_image(imageName):
        imageFileURL = "http://wiki.ubc.ca/File:" + imageName

        connection = urllib.urlopen(imageFileURL)
        dom = lxml.html.fromstring(connection.read())
        for link in dom.xpath('//a/@href'):
            if '//wiki.ubc.ca/images' in link:
                raw_image_url = link
                break

        urllib.urlretrieve(
            "http:" + raw_image_url, os.path.join(directory, imageName))

    def handle_gif(imageName):
        newName = imageName.replace('.gif', '.png')
        fullOldName = os.path.join(directory, imageName)
        fullNewName = os.path.join(directory, newName)
        x = subprocess.check_output(["convert", fullOldName, fullNewName])
        return newName

    def do_string(content_str):
        imageNames = re.findall(
            r"includegraphics{(.*)}", content_str)
        imageNames = [s.replace(u'\xe2\x80\x8e', '') for s in imageNames]
        if not imageNames:
            return content_str
        for imageRawName in imageNames:
            imageName = imageRawName.strip().replace(' ', '_')
            save_image(imageName=imageName)
            if '.gif' in imageName:
                imageName = handle_gif(imageName)
            content_str = content_str.replace('phics{' + imageRawName,
                                              'phics{' + imageName)
            content_str = content_str.replace('width]{' + imageRawName,
                                              'width]{' + imageName)
            content_str = content_str.replace(
                'includegraphics{',
                'includegraphics[width=.5\\textwidth]{')
        return content_str
    try:
        return do_string(content)
    except TypeError:
        return [do_string(c) for c in content]


def json_from_course_exam_question(course, term, year, question):
    exam = term + '_' + str(year)
    directory = os.path.join('json_data', course, exam)
    questionURL = ('/Science:Math_Exam_Resources/'
                   'Courses/' + course + '/' + exam + '/' + question)
    num_hints, num_sols = get_num_hs_question(questionURL)
    question_latex = get_latex_statement_from_url(('http://wiki.ubc.ca' +
                                                   questionURL),
                                                  num_hints, num_sols)

    question = questionURL.split('/')[-1]
    question_json = {"course": course,
                     "year": int(year),
                     "term": term,
                     "question": question,
                     "statement": handleImages(question_latex['statement'],
                                               directory),
                     "hints": handleImages(question_latex['hints'],
                                           directory),
                     "sols": handleImages(question_latex['sols'],
                                          directory)}

    question = question.replace('%2B', '-')
    with open(os.path.join(directory, question + ".json"), "w") as outfile:
        json.dump(question_json, outfile, indent=4)


def json_from_course_exam(course, term, year):
    exam = term + '_' + str(year)
    directory = os.path.join('json_data', course, exam)
    if not os.path.exists(directory):
        os.makedirs(directory)

    question_urls = get_all_questions_from_exam(('http://wiki.ubc.ca/'
                                                 'Science:Math_Exam_Resources/'
                                                 'Courses/' + course + '/' +
                                                 exam))
    for questionURL in question_urls:
        num_hints, num_sols = get_num_hs_question(questionURL)
        question_latex = get_latex_statement_from_url(('http://wiki.ubc.ca' +
                                                       questionURL),
                                                      num_hints, num_sols)

        question = questionURL.split('/')[-1]
        question_json = {"course": course,
                         "year": int(year),
                         "term": term,
                         "question": question,
                         "statement": handleImages(question_latex['statement'],
                                                   directory),
                         "hints": handleImages(question_latex['hints'],
                                               directory),
                         "sols": handleImages(question_latex['sols'],
                                              directory)}
        question = question.replace('%2B', '-')
        with open(os.path.join(directory, question + ".json"), "w") as outfile:
            json.dump(question_json, outfile, indent=4)


def mediawiki_from_edit(input):
    return input.split('name="wpTextbox1">')[1].split('</textarea')[0]


def preCleaning(input):
    input = input.decode('latin1')
    input = input.replace(u'ï¬', 'fi')
    input = input.replace('&amp;', '&')
    input = input.replace('&lt;', '<')
    input = input.replace('&le;', '<math>\leq</math>')
    input = input.replace('&gt;', '>')
    input = input.replace('&ge;', '<math>\geq</math>')
    input = input.replace('&ne;', '<math>\\neq</math>')
    input = re.sub(r'([-+]?)&infin;', r'<math>\1\\infty</math>', input)
    input = input.replace(u'â', '<math>\infty</math>')
    input = input.replace(u'â¥', '<math>\geq</math>')
    input = input.replace(u'â¤', '<math>\leq</math>')
    input = input.replace(u'â', '<math>^{\prime}</math>')
    input = input.replace(u'â²â²', '<math>^{\prime\prime}</math>')
    input = input.replace(u'â²', '<math>^{\prime}</math>')
    input = input.replace(u'â³', '<math>^{\prime\prime}</math>')
    input = input.replace(u'Â°', '<math>^{\circ}</math>')
    input = input.replace(u'â', '')
    input = input.replace(u'â¨', u'<math>\\vee</math>')
    input = input.replace(u'â', u'<math>\\rightarrow</math>')
    input = re.sub(r'&radic;{{overline\|(.*)}}',
                   r'<math>\\sqrt{\1}</math>', input)
    input = re.sub(r'<math> *\\ +', r'<math>', input)
    input = re.sub(r'\\ +</math>', r'</math>', input)
    input = re.sub(r': +<math>', ':<math>', input)
    input = re.sub(r'$\\ (.*)$', r'$\1$', input)
    input = re.sub(r'$\\displaystyle (.*)$', r'$\1$', input)
    input = re.sub(r':<math>\\displaystyle{\\begin{align}(.*)'
                   '\\end{align}}</math>',
                   r':<math>\\begin{align}\1\\end{align}</math>', input)
    input = re.sub(r':<math>\\displaystyle{(.*)}</math>',
                   r':<math>\n\1\n</math>', input)
    return input


def postCleaning(input):
    input = input.replace(u'ƒ', '$f$')
    input = input.replace(u'\u00c6\u0092', '$f$')
    input = input.replace("$f$$^{\prime}$", "$f'$")
    input = input.replace("$f$$^{\prime\prime}$", "$f''$")
    input = input.replace("\emph{f$^{\prime\prime}$}", "$f''$")
    input = re.sub(r"([a-zA-Z)])\s*'",
                   r"\1'", input)
    input = re.sub(r"([a-zA-Z)])\s*'\s*'",
                   r"\1''", input)

    input = re.sub(r"\\color{(.*)\s(.*)}",
                   r"\\color{\1\2}", input)

    input = input.replace(u'\u03b8', '$\\theta$')
    input = input.replace(u'\u03c0', '$\pi$')
    input = input.replace(u'\u03c6', '$\phi$')
    input = input.replace(u'\u03c1', '$\\rho$')
    input = input.replace(u'\u221e', '$\infty$')

    input = re.sub(r'\$\\displaystyle\s*\n*',
                   r'$\\displaystyle ', input)
    input = input.replace("\[\\begin{align}", "\\begin{align*}")
    input = re.sub(r'\$\\displaystyle\s*\\begin{align}',
                   r'\\begin{align*}', input)
    input = input.replace("$\displaystyle\\begin{align}", "\\begin{align*}")
    input = input.replace("\[\displaystyle \\begin{align}", "\\begin{align*}")
    input = input.replace("\[\displaystyle\\begin{align}", "\\begin{align*}")

    input = input.replace("\[\n\\begin{align}", "\\begin{align*}")
    input = input.replace(
        "\[\displaystyle\n\\begin{align*}", "\\begin{align*}")
    input = input.replace("\[\displaystyle\n\\begin{align}", "\\begin{align*}")
    input = input.replace('\[\\begin{alignat}', '\\begin{alignat*}')

    input = input.replace("\end{align}\]", "\end{align*}")
    input = input.replace('\end{align*}\\\\', '\end{align*}\n')
    input = input.replace('\end{align}.\\]', '\end{align*}')
    input = input.replace('\end{alignat}\\]', '\end{alignat*}')

    input = input.replace('$\displaystyle\\begin{align}', '\\begin{align*}')
    input = input.replace('$\\begin{align}', '\\begin{align*}')
    input = input.replace('\\begin{align}', '\\begin{align*}')
    input = input.replace('\end{align}$', '\end{align*}')
    input = input.replace("\\\\]", "\\]")

#    two_newlines_dollar = r'\$([\s\S]*?)\n\n([\s\S]*?)\$'
#    input = re.sub(two_newlines_dollar, r'$\1\2$', input)
#    input = re.sub(two_newlines_dollar, r'$\1\2$', input)
#    two_newlines_align = r'align((?!align)[\s\S]*?)\n\n((?!align)[\s\S]*?)align'
#    input = re.sub(two_newlines_align, r'align\1\2align', input)
#    input = re.sub(two_newlines_align, r'align\1\2align', input)

    input = re.sub('\\begin{align\*}\n+', '\\begin{align*}\n', input)

    input = input.replace('\\toprule\\addlinespace\n', '')
    input = input.replace('\n\\bottomrule', '')
    input = re.sub(r'\n\\+addlinespace', r'\\\\', input)
    input = input.replace('\\addlinespace\n', '')
    input = input.replace('\\\\end{longtable}', '\\end{longtable}')

    input = input.replace('\midrule\endhead', '')
    input = input.replace('style=""\\textbar{} ', '')

    input = re.sub(r'\$(.*)(?<!\\)%(.*)\$', r'$\1\%\2$', input)
    input = re.sub(r"\$f\$('*)\(\\emph{(.)}\)", r"$f\1(\2)$", input)
    input = re.sub(r"([\^_])\\sqrt{([^\s]*)}", r"\1{\\sqrt{\2}}", input)
    input = re.sub(
        r"\[\\displaystyle{\\begin{align\*}([\s\S]*)\\end{align}}\\]",
        r"\\begin{align*}\1\\end{align*}", input)

    input = re.sub(r"\\+begin{align\*}", r'\\begin{align*}', input)
    input = re.sub(r'\\end{align\*}\\+', r'\\end{align*}\n', input)
    input = input.replace('\\\end{align*}', '\end{align*}')
    input = re.sub(r'\n+\\end{align\*}', '\n\end{align*}', input)

    input = re.sub(r'\\\[{\\color{(.*)}\n\\begin{align\*}([\s\S]*)\\end{align}\n}\\\]',
                   r'{\\color{\1}\[\n\\begin{aligned}\2\\end{aligned}\n\]}',
                   input)

    input = re.sub(r'\\\[([\s\S]*)\\begin{align\*}([\s\S]*)\\end{align}',
                   r'\[\1\\begin{aligned}\2\\end{aligned}', input)

    input = re.sub(r'\\]([,.])', r'\1\\]', input)

    return input.strip()


def get_latex_statement_from_url(questionURL, num_hints=1, num_sols=1):

    def get_dict_action_urls(action):
        statementURL = (questionURL.replace(
            "Science", "index.php?title=Science") +
            "/Statement&action=" + action)
        hintURL = (questionURL.replace("Science",
                                       "index.php?title=Science") +
                   "/Hint_")
        solURL = (questionURL.replace("Science",
                                      "index.php?title=Science") +
                  "/Solution_")

        return {'statementURL': statementURL,
                'hintsURLs': [hintURL + str(num + 1) + "&action=" +
                              action for num in range(num_hints)],
                'solsURLs': [solURL + str(num + 1) + "&action=" +
                             action for num in range(num_sols)]}

    def edit_to_latex(shs='statementURL', index=0):
        try:
            out = urllib.urlopen(urls[shs]).read()
        except AttributeError:
            out = urllib.urlopen(urls[shs][index]).read()
        try:
            out = mediawiki_from_edit(out)
        except IndexError:
            warnings.warn('There is a problem with %s. Maybe no content?'
                          % questionURL)
            return 'No content found'

        post_cleaned = postCleaning(pypandoc.convert(preCleaning(out), 'latex',
                                                     format='mediawiki'))
        return post_cleaned

    urls = get_dict_action_urls(action='edit')
    return {'statement': edit_to_latex('statementURL'),
            'hints': [edit_to_latex('hintsURLs', index)
                      for index in range(len(urls['hintsURLs']))],
            'sols': [edit_to_latex('solsURLs', index)
                     for index in range(len(urls['solsURLs']))]}


def get_all_topics():
    MER_URL = 'http://wiki.ubc.ca/Science:Math_Exam_Resources'
    connection = urllib.urlopen(MER_URL)
    dom = lxml.html.fromstring(connection.read())
    searchText = 'MER_Tag_'
    topicLinks = []
    for link in dom.xpath('//a/@href'):  # url in href for tags (are links)
        if searchText in link:
            topicLinks.append(link)
    topicLinks = list(set(topicLinks))
    topicLinks.sort()
    return topicLinks


def write_topics_questions_table():
    outfile = open('raw_topics_questions.csv', 'w')
    outfile.write('%s,%s\n' % ('Topic', 'Question'))
    topics = get_all_topics()
    topics = ['http://wiki.ubc.ca' + t for t in topics]
    for topic in topics:
        questions = get_questionURLs_from_topicURL(topic)
        for q in questions:
            outfile.write('%s,%s\n' % (topic.replace(
                'http://wiki.ubc.ca/Category:MER_Tag_', ''),
                'http://wiki.ubc.ca' + q))
    outfile.close()


def get_questionURLs_from_topicURL(topicURL):
    connection = urllib.urlopen(topicURL)
    dom = lxml.html.fromstring(connection.read())
    searchText = '/Question'
    questionLinks = []
    for link in dom.xpath('//a/@href'):  # url in href for tags (are links)
        if searchText in link:
            questionLinks.append(link)
    questionLinks = list(set(questionLinks))
    return questionLinks


def get_all_courses(MER_URL):
    connection = urllib.urlopen(MER_URL)
    dom = lxml.html.fromstring(connection.read())
    searchText = '/Science:Math_Exam_Resources/Courses/MATH'
    courseLinks = []
    for link in dom.xpath('//a/@href'):  # url in href for tags (are links)
        if searchText in link:
            courseLinks.append(link)
    courseLinks = list(set(courseLinks))
    courseLinks.sort()
    return courseLinks


def get_all_exams_from_course(courseURL):
    connection = urllib.urlopen(courseURL)
    dom = lxml.html.fromstring(connection.read())
    searchTextA = courseURL.split(':')[2] + '/April'
    searchTextD = courseURL.split(':')[2] + '/December'
    examLinks = []
    for link in dom.xpath('//a/@href'):  # url in href for tags (are links)
        if searchTextA in link or searchTextD in link:
            examLinks.append(link)
    return examLinks


def get_all_questions_from_exam(examURL):
    connection = urllib.urlopen(examURL)
    dom = lxml.html.fromstring(connection.read())
    searchText = examURL.split(':')[2] + '/Question'
    questionLinks = []
    for link in dom.xpath('//a/@href'):  # url in href for tags (are links)
        if searchText in link:
            questionLinks.append(link)
    return questionLinks


def get_content_rating_numvotes(questionURL):
    requestURL = questionURL.replace('/Science',
                                     'http://wiki.ubc.ca/Science')
    raw = urllib.urlopen(requestURL).read()
    ratingNumvotes = raw.split('<span id="w4g_rb_area-1">' +
                               'Current user rating: <b>')
    if len(ratingNumvotes) == 2:
        ratingNumvotes = ratingNumvotes[1].split('</span>')[0]
        rating = int(ratingNumvotes.split('/100')[0])
        numvotes = int(ratingNumvotes.split('(')[1].split(' ')[0])
    else:
        rating = None
        numvotes = 0
    return rating, numvotes


def get_num_hs_question(questionURL):
    num_hints = 1
    tryer = True
    while tryer:
        requestURL = ('http://wiki.ubc.ca' + questionURL +
                      '/Hint_' + str(num_hints))
        raw = urllib.urlopen(requestURL).read()
        if 'There is currently no text in this page' in raw:
            tryer = False
            num_hints = num_hints - 1
        else:
            num_hints = num_hints + 1
    num_sols = 1
    tryer = True
    while tryer:
        requestURL = ('http://wiki.ubc.ca' + questionURL +
                      '/Solution_' + str(num_sols))
        raw = urllib.urlopen(requestURL).read()
        if 'There is currently no text in this page' in raw:
            tryer = False
            num_sols = num_sols - 1
        else:
            num_sols = num_sols + 1
    return num_hints, num_sols


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
        question = question.replace('Question_0', '')
        question = question.replace('Question_', '')
        question = question.replace('_', ' ')
        questions.append(question)

        rating, numvote = get_content_rating_numvotes(questionURL)
        ratings.append(rating)
        num_votes.append(numvote)

        num_hint, num_sol = get_num_hs_question(questionURL)
        num_hints.append(num_hint)
        num_sols.append(num_sol)
    return (URLs, courses, exams, questions,
            num_votes, ratings, num_hints, num_sols)


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
        (URL, course, exam, question, num_vote, rating, num_hint,
         num_sol) = create_lists_for_examURLs('http://wiki.ubc.ca' +
                                              examURL)
        URLs.extend(URL)
        courses.extend(course)
        exams.extend(exam)
        questions.extend(question)
        num_votes.extend(num_vote)
        ratings.extend(rating)
        num_hints.extend(num_hint)
        num_sols.extend(num_sol)
    return (URLs, courses, exams, questions,
            num_votes, ratings, num_hints, num_sols)


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
        (URL, course, exam, question, num_vote, rating, num_hint,
         num_sol) = create_lists_for_courseURLs('http://wiki.ubc.ca' +
                                                courseURL)
        URLs.extend(URL)
        courses.extend(course)
        exams.extend(exam)
        questions.extend(question)
        num_votes.extend(num_vote)
        ratings.extend(rating)
        num_hints.extend(num_hint)
        num_sols.extend(num_sol)
    return (URLs, courses, exams, questions,
            num_votes, ratings, num_hints, num_sols)


def main():
    (URLs, courses, exams, questions, num_votes, ratings, num_hints,
     num_sols) = create_lists_for_SQL()
    f = open("raw_data.csv", 'w')
    for u, c, e, q, v, r, h, s in zip(URLs, courses, exams, questions,
                                      num_votes, ratings, num_hints, num_sols):
        f.write("%s,%s,%s,%s,%s,%s,%s,%s\n" % (u, c, e, q, v, r, h, s))
    f.close()

if __name__ == "__main__":
    main()
