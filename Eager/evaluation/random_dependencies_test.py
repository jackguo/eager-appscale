import os
import random
import sys
import time
import yaml

if __name__ == '__main__':
  app_dir = sys.argv[1]
  api_file = sys.argv[2]
  dep_count = int(sys.argv[3])
  print 'Using application directory at: {0}'.format(app_dir)
  print 'Using API file at: {0}'.format(api_file)
  print 'Dependencies per app: {0}'.format(dep_count)

  api_file_handle = open(api_file, 'r')
  api_list = api_file_handle.readlines()
  api_file_handle.close()

  api_count = len(api_list)

  for attempt in (1,2,3):
    print 'Starting attempt {0}'.format(attempt)
    dependencies = set()
    while len(dependencies) < dep_count:
      dependencies.add(random.randint(0, api_count - 1))

    dependencies_yaml = { 'dependencies' : [] }
    for index in dependencies:
      dep = { 'name' : api_list[index].strip(), 'version' : '1.0' }
      dependencies_yaml['dependencies'].append(dep)

    yaml_file = os.path.join(app_dir, 'war', 'WEB-INF', 'dependencies.yaml')
    yaml_file_handle = open(yaml_file, 'w')
    yaml.dump(dependencies_yaml, yaml_file_handle, default_flow_style=False)
    yaml_file_handle.flush()
    yaml_file_handle.close()

    cmd = 'appscale-upload-app --email admin@test.com --file {0} --keyname ' \
          'appscale2187629ee17c43c09b629ee7b0ae084d'.format(app_dir)
    os.system(cmd)
    print 'Starting next iteration in 2 seconds...'
    time.sleep(2)



