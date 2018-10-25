#!/usr/bin/env python

'''
functions.py: It contents example functions.
'''

__author__      = "alvertogit"
__copyright__   = "Copyright 2018"

import sys

def example_function (*args):
    """Function example that process input data and prints a number.

    Args:
        number (int): The integer parameter to be printed.
        
    Raises:
        Exception: If the number of args is lower than 1.
        ValueError: If args[0] is not an integer.
    
    """

    if len(args) < 1:
        raise ValueError("""Error: required arguments <number>""")
    
    try:
        int(args[0])
    except ValueError:
        print('Error: args[0] is not an integer')
        sys.exit(1)
    
    number = int(args[0])
    print('Number: {0}'.format(number))
    