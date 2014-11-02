# MER2latex
import os
import pandas as pd
import json
import re
import argparse
import sys
import subprocess
import glob


def correct_image_path(text, image_path):
    text = re.sub(r'includegraphics(.*){(.*)}',
                  r'includegraphics\1{ADDPATHHERE/\2}',
                  text)
    text = text.replace('ADDPATHHERE', image_path)
    return text


def file_loc_from_question_url(url):
    course, exam, q_name = url.split('/')[5:8]
    file_loc = os.path.join('json_data', course, exam, q_name + '.json')
    return file_loc


def latex_header():
    def packages():
        return '\n'.join(['\usepackage{amsmath}',
                          '\usepackage{amssymb}',
                          '\usepackage{amsfonts}',
                          '\usepackage{graphicx}',
                          '\usepackage{fixltx2e}',
                          '\usepackage{hyperref}',
                          '\usepackage[usenames,dvipsnames,svgnames]{xcolor}',
                          '\usepackage{longtable}',
                          '\usepackage[margin=2.5cm]{geometry}'
                          ])

    def colors():
        return '\n'.join(['\definecolor{main_Color}{HTML}{2F4F4F}',
                          '\definecolor{secondary_Color1}{HTML}{666666}',
                          '\definecolor{secondary_Color2}{HTML}{7491A3}'
                          ])

    def math_operators():
        return '\n'.join(['\\newcommand{\R}{\mathbb{R}}',
                          '\\newcommand{\C}{\mathbb{C}}',
                          '\\newcommand{\N}{\mathbb{N}}',
                          '\\newcommand{\Z}{\mathbb{Z}}',
                          '\\DeclareMathOperator{\\arcsec}{arcsec}',
                          '\\DeclareMathOperator{\\arccot}{arccot}',
                          '\\DeclareMathOperator{\\arccsc}{arccsc}'
                          ])

    def document_settings():
        return '\n'.join(['\setlength{\parindent}{0pt}',
                          '\\setcounter{secnumdepth}{-2}'
                          ])

    def MER_environments():
        def MER_document_title():
            return '\\newcommand{\MERDocumentTitle}[1]{\section{#1}}'

        def MER_question_title():
            return '\\newcommand{\MERQuestionTitle}[1]{\section{#1}}'

        def MER_easiness():
            return ('\\newcommand{\MEREasiness}[1]{' +
                    '\quad \hfill \\textbf{Easiness: #1/100}\\\\}')

        def MER_statement():
            return '\n'.join(['\\newenvironment{MERStatement}',
                              ('{\\fcolorbox{main_Color}{main_Color}' +
                               '{\color{white}\\textsc{Statement.}}' +
                               ' \color{main_Color} }'),
                              ('{\hspace{\stretch{1}}\color{main_Color}' +
                               '\\rule{10ex}{1ex}\medskip}')
                              ])

        def MER_hint():
            return '\n'.join(['\\newenvironment{MERHint}[1]',
                              ('{\\fcolorbox{secondary_Color1}' +
                               '{secondary_Color1}{\\textsc{Hint#1.}\\\\}}'),
                              ('{\hspace{\stretch{1}}' +
                               '{\color{secondary_Color1}' +
                               '\\rule{10ex}{1ex}}\medskip\\\\}')
                              ])

        def MER_solution():
            return '\n'.join(['\\newenvironment{MERSolution}[1]',
                              ('{\\fcolorbox{secondary_Color2}' +
                               '{secondary_Color2}' +
                               '{\\textsc{Solution#1.}\\\\}}'),
                              ('{\hspace{\stretch{1}}' +
                               '{\color{secondary_Color2}' +
                               '\\rule{10ex}{1ex}}\medskip\\\\}')
                              ])

        return '\n'.join([MER_document_title(),
                          MER_question_title(),
                          MER_easiness(),
                          MER_statement(),
                          MER_hint(),
                          MER_solution()])

    return '\n'.join(['\documentclass{article}',
                      packages(),
                      colors(),
                      math_operators(),
                      document_settings(),
                      MER_environments(),
                      '\\begin{document}'
                      ])


def document_title(title):
    return '\MERDocumentTitle{%s}\n' % (title.replace('_', ' '))


def question_title(course, exam, q_name_human):
    return '\MERQuestionTitle{%s - %s - %s}\n' % (course,
                                                  exam.replace('_', ' '),
                                                  q_name_human)


def easiness(easiness_raw):
    try:
        rating = int(easiness_raw[0])
        return '\MEREasiness{%s}\n' % rating
    except (IndexError, ValueError):
        return ''


def statement(content):
    return '\n'.join(['\\begin{MERStatement}',
                      content,
                      '\end{MERStatement}',
                      '\n'])


def hints_sols(hints_sols_list, hint_or_sol):
    def single(content, num=1, show=False):
        begin = '\\begin{MER%s}' % hint_or_sol
        if show:
            single_num = ' ' + str(num)
        else:
            single_num = ''
        end = '\end{MER%s}' % hint_or_sol
        return '\n'.join([begin + '{' + single_num + '}',
                          content,
                          end])
    result_str = ''
    if len(hints_sols_list) == 1:
        result_str += single(hints_sols_list[0], show=False)
    else:
        for n, h in enumerate(hints_sols_list):
            result_str += single(h, num=n + 1, show=True) + '\n'
    return result_str


def write_question(data, image_path, easiness_raw):
    result_str = ''
    course = data['course']
    term = data['term']
    year = str(data['year'])
    exam = '_'.join([term, year])
    q_name = data['question'].replace('_', ' ')
    q_name_human = q_name.replace('_',
                                  '').replace('Question 0',
                                              'Q ').replace('Question ',
                                                            'Q ')
    result_str += question_title(course, exam, q_name_human)
    result_str += easiness(easiness_raw)
    result_str += correct_image_path(statement(data['statement']), image_path)
    result_str += hints_sols([correct_image_path(h, image_path)
                              for h in data['hints']], 'Hint')
    result_str += hints_sols([correct_image_path(s, image_path)
                              for s in data['sols']], 'Solution')

    return result_str


def write_latex(file_list, title, where_to_save):
    df = pd.read_csv('raw_data.csv', header=None,
                     names=['url', 'course', 'exam', 'q_short',
                            'num_votes', 'rating', 'num_hints',
                            'num_sols'])

    out = open(
        os.path.join(where_to_save, title.replace(' ', '_') + '.tex'), 'w')
    out.write(latex_header())
    out.write(document_title(title))

    for f in file_list:
        fd = open(f, 'r')
        data = json.loads(fd.read())
        fd.close()
        course, exam = f.split('/')[-3:-1]
        easiness_raw = list(df['rating'][
            df.url.str.contains(
                f.replace('json_data/', '').replace('.json', '').
                replace('(', '\(').replace(')', '\)'))
        ])
        image_path = os.path.join('..', 'json_data', course, exam)
        out.write(write_question(data, image_path, easiness_raw))
    out.write('\n\end{document}')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='MER topic or exam to latex')
    parser.add_argument('--topic', dest='topic', default='/',
                        help='filter on topic')
    parser.add_argument('--course', dest='course', default='/',
                        help='filter on course')
    parser.add_argument('--exam', dest='exam', default='/',
                        help='filter on exam')
    args = parser.parse_args()

    title = ''
    if not args.topic == '/':
        df = pd.read_csv('raw_topics_questions.csv')
        df['loc'] = df['Question'].apply(file_loc_from_question_url)
        file_locs = df['loc'][((df.Topic == args.topic) &
                               (df.Question.str.contains(args.course)) &
                               (df.Question.str.contains(args.exam)))].order()
        title += args.topic
        if not args.course == '/':
            title += ' from %s' % args.course
            if not args.exam == '/':
                title += ' - %s' % args.exam
    else:
        df = pd.read_csv('raw_data.csv', header=None,
                         names=['url', 'course', 'exam', 'q_short', 'num_votes',
                                'rating', 'num_hints', 'num_sols'])
        df['loc'] = df['url'].apply(file_loc_from_question_url)
        file_locs = df['loc'][((df.url.str.contains(args.course)) &
                               (df.url.str.contains(args.exam)))].order()
        if not args.course == '/':
            title += args.course
            if not args.exam == '/':
                title += ' - %s' % args.exam

    if '/' == args.topic and '/' == args.course and '/' == args.exam:
        print('ERROR: Must specify at least one of --topic, --course, --exam')
        sys.exit()

    write_latex(list(file_locs), title, os.path.join('test'))

    directory = os.path.join('test')
    os.chdir(directory)
    x = subprocess.check_output(
        ["pdflatex", "%s.tex" % title.replace(' ', '_')])
    x = subprocess.check_output(
        ["pdflatex", "%s.tex" % title.replace(' ', '_')])
    for ending in ['log', 'aux', 'out', 'toc']:
        for doomed in glob.glob('*' + ending):
            os.remove(doomed)
    print('Finished')
