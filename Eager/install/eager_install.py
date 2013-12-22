#!/usr/bin/python

import getpass
import os

MYSQL_ROOT_PASS = 'mysql_root_pass'
MYSQL_USER = 'mysql_user'
MYSQL_PASS = 'mysql_pass'
AM_USER = 'am_user'
AM_PASS = 'am_pass'

def get_input(prop, default=None):
  if default:
    input = raw_input('Enter {0} (default: {1}) : '.format(prop, default))
  else:
    input = raw_input('Enter {0} : '.format(prop))

  if input:
    return input
  elif default:
    return default
  else:
    print 'Property {0} is required.'.format(prop)
    return get_input(prop, default)

def get_password_input(prop):
  pass1 = getpass.getpass('Enter {0} : '.format(prop))
  pass2 = getpass.getpass('Confirm {0} : '.format(prop))
  if pass1 == pass2:
    return pass1
  else:
    print 'Entries do not match. Please try again.'
    return get_password_input(prop)

def get_user_input():
  mysql_root_pass = get_password_input('MySQL root password')

  mysql_user = get_input('MySQL user', 'root')
  if mysql_user != 'root':
    mysql_pass = get_password_input('MySQL password')
  else:
    mysql_pass = mysql_root_pass

  am_admin = get_input('API Manager admin user', 'admin')
  am_pass = get_password_input('API Manager admin password')

  while True:
    print
    print 'MYSQL root password:', '*' * len(mysql_root_pass)
    print 'MySQL user:', mysql_user
    print 'MySQL password:', '*' * len(mysql_pass)
    print 'API Manager admin user:', am_admin
    print 'API Manager admin password:', '*' * len(am_pass)
    print
    proceed = raw_input('Proceed with above settings? (yes/no)\n')
    if proceed == 'yes':
      result = {
        MYSQL_ROOT_PASS : mysql_root_pass,
        MYSQL_USER : mysql_user,
        MYSQL_PASS : mysql_pass,
        AM_USER : am_admin,
        AM_PASS : am_pass,
      }
      return result
    elif proceed == 'no':
      return None
    else:
      print 'Please enter either "yes" or "no"'

def setup_mysql(inputs):
  status = os.system("sh setup_mysql.sh '{0}' '{1}' '{2}'".format(inputs[MYSQL_ROOT_PASS],
    inputs[MYSQL_USER], inputs[MYSQL_PASS]))
  if status:
    print 'MySQL setup failed. Aborting...'
    exit(1)

def setup_api_manager(inputs):
  pass

if __name__ == '__main__':
  inputs = None
  while not inputs:
    inputs = get_user_input()
    print
  setup_mysql(inputs)
  setup_api_manager(inputs)
