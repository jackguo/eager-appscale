#!/usr/bin/python

import copy
import json
import os
import random
import sys
from eager import Eager
from utils import utils

def load_template_specification():
  current_dir = os.path.dirname(os.path.abspath(__file__))
  full_path = os.path.join(current_dir, 'tests', 'samples', '1.json')
  file_handle = open(full_path, 'r')
  spec = json.load(file_handle)
  file_handle.close()
  return spec

def get_api_list(api_id, spec):
  api_list = []
  toss = random.randint(0, 9)
  if toss >= 8:
    return api_list
  count = random.randint(1,3)
  while True:
    if count <= api_id:
      break
    else:
      count = random.randint(1,3)
  for item in range(count):
    new_spec = copy.deepcopy(spec)
    new_spec['apiName'] = 'SampleApi' + str(api_id)
    api = {
      'name' : 'SampleApi' + str(api_id),
      'version' : '1.0',
      'specification' : new_spec
    }
    api_list.append(api)
    api_id -= 1
  return api_list

def get_dependencies(api_id, max_api_id):
  dependencies = []
  if api_id < max_api_id:
    toss = random.randint(0,9)
    if toss == 9:
      dep_count = random.randint(1,3)
      used_dep_ids = set()
      for item in range(dep_count):
        current_dep_id = -1
        while True:
          dep_id = random.randint(api_id + 1, max_api_id)
          if dep_id not in used_dep_ids:
            used_dep_ids.add(dep_id)
            current_dep_id = dep_id
            break
        dep = {
          'name' : 'SampleApi' + str(current_dep_id),
          'version' : '1.0'
        }
        dependencies.append(dep)
  return dependencies

def add_application(eager_client, secret, app):
  response = eager_client.validate_application_for_deployment(secret, app)
  if response['success']:
    print 'Application {0} validated successfully'.format(app['name'])
    for api in app['api_list']:
      pub_response = eager_client.publish_api(secret, api, 'http://192.168.33.101:8080')
      if not pub_response['success']:
        print 'Failed to publish API {0}: {1}'.format(api['name'], pub_response['reason'])
  else:
    print 'Failed to validate application {0}: {1}'.format(app['name'], response['reason'])

if __name__ == '__main__':
  MAX_API_COUNT = 1
  if len(sys.argv) > 1:
    MAX_API_COUNT = int(sys.argv[1])
    print 'Generating test data for {0} sample APIs'.format(MAX_API_COUNT)

  secret = utils.get_secret()
  eager = Eager()
  result, message = eager.ping(secret)
  if result:
    print 'EAGER service is alive - Continuing with the data population...'
  else:
    print 'EAGER service is down or faulty - Aborting...'
    exit(1)

  api_id = MAX_API_COUNT
  app_count = 1

  spec = load_template_specification()

  while api_id > 0:
    api_list = get_api_list(api_id, spec)
    app = {
      'name' : 'Application' + str(app_count),
      'version' : '1.0',
      'api_list' : api_list,
      'dependencies' : get_dependencies(api_id, MAX_API_COUNT),
      'owner' : 'admin@test.com'
    }
    api_id -= len(api_list)
    app_count += 1
    add_application(eager, secret, app)
    print