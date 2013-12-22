import getpass

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
  return getpass.getpass('Enter {0} : '.format(prop))

def get_user_input():
  mysql_user = get_input('MySQL user', 'root')
  mysql_pass = get_password_input('MySQL password')

  am_admin = get_input('API Manager admin user', 'admin')
  am_pass = get_password_input('API Manager admin password')

  while True:
    print 'MySQL user:', mysql_user
    print 'MySQL password:', '*' * len(mysql_pass)
    print 'API Manager admin user:', am_admin
    print 'API Manager admin password:', '*' * len(am_pass)
    print
    proceed = raw_input('Proceed with above settings? (yes/no)\n')
    if proceed == 'yes':
      result = {
        'mysql_user': mysql_user,
        'mysql_pass': mysql_pass,
        'am_admin': am_admin,
        'am_pass': am_pass,
      }
      return result
    elif proceed == 'no':
      return None
    else:
      print 'Please enter either "yes" or "no"'

def setup_mysql(inputs):
  pass

def setup_api_manager(inputs):
  pass

if __name__ == '__main__':
  inputs = None
  while not inputs:
    inputs = get_user_input()
  setup_mysql(inputs)
  setup_api_manager(inputs)
