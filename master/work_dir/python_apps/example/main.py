#!/usr/bin/env python

'''
main.py: It executes an example function.
'''

__author__      = "alvertogit"
__copyright__   = "Copyright 2018-2022"

import sys

from functions import example_function

if __name__ == '__main__':

    arguments = sys.argv[1:]
    print("Executing example_function")
    example_function(*arguments)
    print("Ending example_function")
