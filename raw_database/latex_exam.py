import os
import json
from os import listdir
from os.path import isfile, join
from MER2csv import json_from_course_exam
import sys
import subprocess


def download_json(course, term, year):
    json_from_course_exam(course, term, year)


def writeLatex(course, term, year):
    exam = term + '_' + str(year)
    directory = os.path.join('json_data', course, exam)

    onlyfiles = [f for f in listdir(directory) if isfile(
        join(directory, f)) and '.json' in f]
    onlyfiles.sort()

    out = open(os.path.join(directory, 'full_exam.tex'), 'w')
    out.write('\documentclass{article}\n'
              '\usepackage{amsmath}\n'
              '\usepackage{amssymb}\n'
              '\usepackage{amsfonts}\n'
              '\usepackage{graphicx}\n'
              '\usepackage{fixltx2e}\n'
              '\usepackage[draft]{hyperref}\n'
              '\usepackage{color}\n'
              '\\newcommand{\R}{\mathbb{R}}'
              '\\newcommand{\C}{\mathbb{C}}'
              '\\newcommand{\N}{\mathbb{N}}'
              '\\newcommand{\Z}{\mathbb{Z}}'
              '\\begin{document}\n')

    out.write('\section*{%s - %s %s}' % (course, term, year))
    for f in onlyfiles:
        fd = open(os.path.join(directory, f), 'r')
        text = fd.read()
        fd.close()
        data = json.loads(text)
        qname = data['question'].replace('_', ' ')
        out.write('\\textbf{' + qname + '}')
        out.write('\n')
        statement = data['statement']
        out.write(statement)
        hints = data['hints']
        if len(hints) == 1:
            out.write(
                '\n\n\\bigskip \n \\textbf{Hint.} \n %s \n\n' % hints[0])
        else:
            for num, hint in enumerate(hints):
                out.write(
                    '\n\n\\bigskip \n'
                    '\\textbf{Hint %s.}'
                    '\n %s \n\n' % (num + 1, hint))
        sols = data['sols']
        if len(sols) == 1:
            out.write(
                '\n\n\\bigskip \n \\textbf{Solution.} \n %s \n\n' % sols[0])
        else:
            for num, sol in enumerate(sols):
                out.write(
                    '\n\n\\bigskip \n'
                    '\\textbf{Solution %s.}'
                    '\n %s \n\n' % (num + 1, sol))
        out.write('\n \n \\bigskip \\noindent'
                  '\makebox[\linewidth]{\\rule{0.6\paperwidth}{0.4pt}}'
                  '\\bigskip \n\n')

    out.write('\end{document}')
    out.close()


if __name__ == '__main__':
    if not 4 == len(sys.argv):
        errorMsg = ("MUST CALL WITH 3 Arguments:"
                    "course (MATH100), "
                    "term (December), "
                    "year (2013)")
        raise Exception(errorMsg)
    course = sys.argv[1]
    term = sys.argv[2]
    year = sys.argv[3]
    download_json(course, term, year)
    writeLatex(course, term, year)

    print('Done downloading from MER. Start compiling LaTeX...')

    exam = term + '_' + str(year)
    directory = os.path.join('json_data', course, exam)
    os.chdir(directory)
    x = subprocess.check_output(
        ["pdflatex", "--interaction nonstopmode", "full_exam.tex"])
    os.remove("full_exam.log")
    os.remove("full_exam.aux")
    print('Finished')
