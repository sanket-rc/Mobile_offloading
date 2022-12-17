#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Tue Aug 30 11:34:15 2022

@author: zamaan
"""
import os
import shutil
from flask import Flask, request
from werkzeug.utils import secure_filename

from mnist_classification import run_example, define_model

FILE_TYPES = {'png', 'jpg', 'jpeg'}

app = Flask(__name__)

def check_file_type(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in FILE_TYPES

@app.route('/trainAndTestModel')
def trainAndTest():
    define_model()
    return 'Training and testing completed'


@app.route('/uploadImage', methods=['POST'])
def upload_file():
    # category = request.form['category']
    if 'file' not in request.files:
        return 'file part is missing'
    file = request.files['file']
    if file and check_file_type(file.filename):

        filename = secure_filename(file.filename)
        filepath = os.path.join(os.getcwd(), filename)
        file.save(filepath)

        category = str(run_example(filename))

        categories = os.listdir(os.path.join(os.getcwd(),'classification_results'))
        if category not in categories:
            os.mkdir(os.path.join(os.getcwd(),'classification_results', category))
        
        # file.save(os.path.join(os.getcwd(),'classification_results', category, filename))

        shutil.move(filename, os.path.join(os.getcwd(),'classification_results', category, filename))

        return f'classification successful : {category}'


if __name__ == '__main__':
    app.run(host="0.0.0.0",port=5001)