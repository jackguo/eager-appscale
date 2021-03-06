#!/usr/bin/python

import getpass
import os
import yaml

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
  status = os.system("sh setup_mysql.sh '{0}'".format(inputs[MYSQL_ROOT_PASS]))
  if status:
    print 'MySQL setup failed. Aborting...'
    exit(1)

def setup_api_manager(inputs):
  file_path = '/root/wso2am-1.4.0.zip'
  if os.path.exists(file_path):
    status = os.system("sh setup_am.sh '{0}' '{1}' '{2}' '{3}'".format(file_path,
      inputs[MYSQL_ROOT_PASS], inputs[MYSQL_USER], inputs[MYSQL_PASS]))
    if status:
      print 'API Manager setup failed. Aborting...'
      exit(1)

    print 'Updating data source configuration...'
    with open('master-datasources.xml', 'r') as ds_file:
      content = ds_file.read().replace('${mysql.user}',
        inputs[MYSQL_USER]).replace('${mysql.password}', inputs[MYSQL_PASS])
      output_file = open('/root/APIManager/repository/conf/datasources/master-datasources.xml', 'w')
      output_file.write(content)
      output_file.flush()
      output_file.close()

    print 'Updating user manager configuration...'
    with open('user-mgt.xml', 'r') as um_file:
      content = um_file.read().replace('${am.user}',
        inputs[AM_USER]).replace('${am.password}', inputs[AM_PASS])
      output_file = open('/root/APIManager/repository/conf/user-mgt.xml', 'w')
      output_file.write(content)
      output_file.flush()
      output_file.close()

    print 'Updating Carbon configuration...'
    with open('carbon.xml', 'r') as carbon_file:
      content = carbon_file.read().replace('${am.user}', inputs[AM_USER])
      output_file = open('/root/APIManager/repository/conf/carbon.xml', 'w')
      output_file.write(content)
      output_file.flush()
      output_file.close()

    print 'Writing eager.yaml file'
    conf = { 'api_manager' : {} }
    conf['api_manager']['provider'] = 'wso2am1.4'
    conf['api_manager']['host'] = 'localhost'
    conf['api_manager']['port'] = 9443
    conf['api_manager']['user'] = inputs[AM_USER]
    conf['api_manager']['password'] = inputs[AM_PASS]
    eager_conf = open('/root/appscale/Eager/eager.yaml', 'w')
    yaml.dump(conf, eager_conf, default_flow_style=False)
    eager_conf.flush()
    eager_conf.close()

    print 'All done.'
  else:
    print 'Failed to locate API Manager archive in /root. Aborting...'
    exit(1)

if __name__ == '__main__':
  inputs = None
  print 'Welcome to EAGER installation for AppScale.'
  print """
This program will help you install EAGER along with MySQL and WSO2 API Manager
(v1.4). You will be asked to specify a root password for MySQL. If MySQL is not
already installed, it will be installed and configured with the specified root
password. If MySQL is already installed, you should enter the root password for
the existing MySQL set up.
"""
  while not inputs:
    inputs = get_user_input()
    print
  setup_mysql(inputs)
  print
  setup_api_manager(inputs)
