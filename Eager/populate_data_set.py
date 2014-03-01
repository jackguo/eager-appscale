#!/usr/bin/python

import sys
from eager import Eager
from generate_test_data import load_template_specification, add_application
from utils import utils

if __name__ == '__main__':
  secret = utils.get_secret()
  eager = Eager()
  spec = load_template_specification()

  file_name = sys.argv[1]
  print 'Loading data set from {0}'.format(file_name)
  fh = open(file_name, 'r')
  lines = fh.readlines()
  fh.close()

  for	line in	lines:
    line = line.replace('\n', '')
    if not line.startswith('  -->'):
      index = line.index(' /api/')
      api_name = line[index + 6:]

      spec['apiName'] = api_name
      api = {
        'name' : api_name,
        'version' : '1.0',
        'specification' : spec
      }

      app = {
        'name' : '{0}_app'.format(api_name),
        'version' : '1.0',
        'api_list' : [ api ],
        'dependencies' : [],
        'owner' : 'admin@test.com'
      }
      add_application(eager, secret, app)
