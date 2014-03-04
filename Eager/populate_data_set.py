#!/usr/bin/python
import string

import sys
from eager import Eager
from generate_test_data import load_template_specification, add_application
from utils import utils

def add_api_set(lines, eager, secret, spec):
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

def add_mashup_set(lines, eager, secret, spec):
  last_api = None
  mashups = {}
  print 'Calculating API dependencies...'
  for	line in	lines:
    line = line.replace('\n', '')
    if not line.startswith('  -->'):
      index = line.index(' /api/')
      last_api = line[index + 6:]
    else:
      mashup_name = line[6:]
      dep = {
        'name' : last_api,
        'version' : '1.0'
      }
      if mashups.get(mashup_name):
        mashups[mashup_name].append(dep)
      else:
        mashups[mashup_name] = [ dep ]

  print 'Found {0} APIs with dependencies'.format(len(mashups))

  count = 1
  for k in sorted(mashups.keys()):
    v = mashups[k]
    api_name = 'm{0}_{1}'.format(count, cleanup_string(k))
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
      'dependencies' : v,
      'owner' : 'admin@test.com'
    }
    add_application(eager, secret, app)
    count += 1

def cleanup_string(s):
  s = string.translate(s.lower(), None, "'/ &+*@%\"<>!,#;^(){}|~=")
  return ''.join([i if ord(i) < 128 else '' for i in s])

if __name__ == '__main__':
  secret = utils.get_secret()
  eager = Eager()
  spec = load_template_specification()

  file_name = sys.argv[1]
  print 'Loading data set from {0}'.format(file_name)
  fh = open(file_name, 'r')
  lines = fh.readlines()
  fh.close()

  add_api_set(lines, eager, secret, spec)
  add_mashup_set(lines, eager, secret, spec)
