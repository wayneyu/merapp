import os
import json
from os import listdir
from os.path import isfile, join
from MER2csv import get_all_topics, get_questionURLs_from_topicURL, get_num_hs_question
import sys
import subprocess
import re


def get_topics():
    return get_all_topics()


def correct_image_path(text, image_path):
    text = re.sub(r'includegraphics(.*){(.*)}',
                  r'includegraphics\1{ADDPATHHERE/\2}',
                  text)
    text = text.replace('ADDPATHHERE', image_path)
    return text


def gather_exam(where, title, files):
    if not os.path.exists(where):
        os.makedirs(where)
    out = open(os.path.join(where, title + '.tex'), 'w')
    out.write('\documentclass{article}\n'
              '\usepackage{amsmath}\n'
              '\usepackage{amssymb}\n'
              '\usepackage{amsfonts}\n'
              '\usepackage{graphicx}\n'
              '\usepackage{fixltx2e}\n'
              '\usepackage{hyperref}\n'
              '\usepackage{color}\n'
              '\usepackage{longtable}\n'
              '\\newcommand{\R}{\mathbb{R}}\n'
              '\\newcommand{\C}{\mathbb{C}}\n'
              '\\newcommand{\N}{\mathbb{N}}\n'
              '\\newcommand{\Z}{\mathbb{Z}}\n'
              '\\DeclareMathOperator{\\arcsec}{arcsec}\n'
              '\\DeclareMathOperator{\\arccot}{arccot}\n'
              '\\DeclareMathOperator{\\arccsc}{arccsc}\n'
              '\\setcounter{secnumdepth}{-2}\n'
              '\\begin{document}\n')
    out.write('\section{%s}\n' % title.replace('_', ' '))
    out.write('\\tableofcontents\n')
    for f in files:
        fd = open(f, 'r')
        text = fd.read()
        course, exam = f.split('/')[-3:-1]
        image_path = os.path.join('..', course, exam)
        fd.close()
        data = json.loads(text)
        qname = data['question'].replace('_', ' ')
        q_name_human = qname.replace('_',
                                     '').replace('Question 0',
                                                 'Q ').replace('Question ',
                                                               'Q ')
        out.write('\\section{%s - %s - %s}' % (course,
                                               exam.replace('_', ' '),
                                               q_name_human))
        out.write('\n')
        statement = correct_image_path(data['statement'], image_path)

        out.write(statement)
        hints = [correct_image_path(h, image_path) for h in data['hints']]
        if len(hints) == 1:
            out.write(
                '\n\n\\bigskip \n \\subsection{Hint} \n %s \n\n' % hints[0])
        else:
            for num, hint in enumerate(hints):
                out.write(
                    '\n\n\\bigskip \n'
                    '\\subsection{Hint %s}'
                    '\n %s \n\n' % (num + 1, hint))
        sols = [correct_image_path(s, image_path) for s in data['sols']]
        if len(sols) == 1:
            out.write(
                '\n\n\\bigskip \n \\subsection{Solution} \n %s \n\n' % sols[0])
        else:
            for num, sol in enumerate(sols):
                out.write(
                    '\n\n\\bigskip \n'
                    '\\subsection{Solution %s}'
                    '\n %s \n\n' % (num + 1, sol))
        out.write('\n \n \\bigskip \\noindent'
                  '\makebox[\linewidth]{\\rule{0.6\paperwidth}{0.4pt}}'
                  '\\bigskip \n\n')

    out.write('\end{document}')
    out.close()


def write_topic(topic):
    questions = get_questionURLs_from_topicURL(topic)
    questions = [q for q in questions if (get_num_hs_question(q)[0] > 0 and
                                          get_num_hs_question(q)[1] > 0)]
    files = []
    for q in questions:
        course, exam, q_name = q.split('/')[3:6]
        file_loc = os.path.join('json_data', course, exam, q_name + '.json')
        files.append(file_loc)
    files = [f for f in files if isfile(f)]
    topic_human = topic.replace('%27', '').replace(
        'http://wiki.ubc.ca/Category:MER_Tag_', '')
    directory = os.path.join('json_data', 'topics')
    gather_exam(directory, topic_human, files)

if __name__ == '__main__':
    #    topics = get_topics()
    #    topics = ['http://wiki.ubc.ca' + t for t in topics]
    #
    #    for topic in topics:
    #        print(topic)
    #        write_topic(topic)
    directory = os.path.join('json_data', 'topics')
    os.chdir(directory)
    #    #
    #    onlyfiles = os.listdir('.')
    #    for f in onlyfiles:
    #        if f.endswith('.tex'):
    #            print(f)
    #            x = subprocess.check_output(
    #                ["pdflatex", f])
    #            x = subprocess.check_output(
    #                ["pdflatex", f])

    files = os.listdir('.')
    for f in files:
        if (f.endswith(".log") or f.endswith('.aux')
                or f.endswith('.out') or f.endswith('.toc')):
            os.remove(f)
