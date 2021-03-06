import copy
from apimgt.typechecker import *

def validate_swagger_description(spec):
  errors = []
  if not spec.get('swaggerVersion'):
    errors.append("Missing 'swaggerVersion'")
  if not spec.get('apiName'):
    errors.append("Missing 'apiName'")
  if not spec.get('apiVersion'):
    errors.append("Missing 'apiVersion'")
  if not spec.get('basePath'):
    errors.append("Missing 'basePath'")
  if not spec.get('resourcePath'):
    errors.append("Missing 'resourcePath'")

  if not spec.get('apis'):
    errors.append("Missing 'apis'")
  elif not isinstance(spec['apis'], list):
    errors.append("Invalid 'apis' section")
  else:
    api = spec['apis'][0]
    if not api.get('operations'):
      errors.append("Missing 'operations' in API")
    elif not isinstance(api['operations'], list):
      errors.append("Invalid 'operations' section")
    else:
      for op in api['operations']:
        if not op.get('nickname'):
          errors.append("Missing 'nickname' in operation")
        if not op.get('method'):
          errors.append("Missing 'method' in operation")
        if op.get('type') and not is_valid_type(op['type'], spec):
          errors.append("Undefined data type: {0}".format(op['type']))

        params = op.get('parameters')
        if params:
          for param in params:
            if not param.get('name'):
              errors.append("Missing 'name' in parameter")
            if not param.get('paramType'):
              errors.append("Missing 'paramType' in parameter")
            elif param['paramType'] not in ('form', 'body', 'query', 'header'):
              errors.append("Invalid 'paramType' value: {0}".format(param['paramType']))
            elif param['paramType'] == 'body' and not param.get('required'):
              errors.append("'required' must be set to true for 'body' parameters")
            if not param.get('dataType'):
              errors.append("Missing 'dataType' in parameter")
            elif not is_valid_type(param['dataType'], spec):
              errors.append("Undefined data type: {0}".format(param['dataType']))

  if errors:
    return False, '|'.join(errors)
  else:
    return True, None

def is_valid_type(type_name, spec):
  if type_name in PrimitiveType.PRIMITIVES:
    return True
  models = spec.get('models')
  if models:
    for key,model in models.items():
      if model['id'] == type_name:
        return True
  return False

def is_api_compatible(old_spec, new_spec, ops=list()):
  old_api = old_spec['apis'][0]
  new_api = copy.deepcopy(new_spec['apis'][0])

  errors = []

  if old_spec['basePath'] != new_spec['basePath']:
    errors.append("Base paths are incompatible: Old API={0}; New API={1}".format(
      old_spec['basePath'], new_spec['basePath']))

  if old_spec['resourcePath'] != new_spec['resourcePath']:
    errors.append("Resource paths are incompatible: Old API={0}; New API={1}".format(
      old_spec['resourcePath'], new_spec['resourcePath']))

  for old_operation in old_api['operations']:
    if is_related_operation(old_operation, ops):
      new_operation = find_operation(new_api, old_operation)
      if not new_operation:
        errors.append("Operation '{0}' not present in the new API".format(old_operation['nickname']))
      else:
        errors += compare_operations(old_operation, old_spec, new_operation, new_spec)
        new_api['operations'].remove(new_operation)

  if not errors:
    return True, None
  else:
    return False, '|'.join(errors)

def compare_operations(old_op, old_spec, new_op, new_spec):
  errors = []
  op_name = old_op['nickname']
  if old_op['method'] != new_op['method']:
    errors.append("HTTP method incompatibility in operation '{0}': Old API = {1}; " \
                  "New API = {2}".format(op_name, old_op['method'], new_op['method']))
  errors += compare_mime_types(old_op, new_op)
  errors += compare_input_params(old_op, old_spec, new_op, new_spec)
  errors += compare_output_types(old_op, old_spec, new_op, new_spec)
  return errors

def compare_mime_types(old_op, new_op):
  errors = []
  old_consumes = old_op.get('consumes', [])
  new_consumes = new_op.get('consumes', [])
  for mime in old_consumes:
    if mime not in new_consumes:
      errors.append("Input media type '{0}' is not supported by the new API".format(mime))

  old_produces = old_op.get('produces', [])
  new_produces = new_op.get('produces', [])
  for mime in old_produces:
    if mime not in new_produces:
      errors.append("Output media type '{0}' is not supported by the new API".format(mime))

  return errors

def compare_input_params(old_op, old_spec, new_op, new_spec):
  errors = []
  old_params = old_op.get('parameters')
  new_params = new_op.get('parameters')
  if not new_params:
    return errors

  if old_params:
    for old_p in old_params:
      new_p = find_parameter(new_params, old_p)
      if not new_p:
        continue
      if old_p['paramType'] != new_p['paramType']:
        errors.append("Type of the parameter '{0}' has changed: Old API = {1}; " \
                      "New API = {2}".format(old_p['name'], old_p['paramType'], new_p['paramType']))
      if not old_p.get('required') and new_p.get('required'):
        errors.append("Optional parameter '{0}' has been made mandatory in the " \
                      "new API".format(old_p['name']))
      old_param_type = swagger_to_eager_type(old_p['dataType'], old_spec)
      new_param_type = swagger_to_eager_type(new_p['dataType'], new_spec)
      compatible = is_input_compatible(old_param_type, new_param_type, errors)
      if not compatible:
        errors.append("Types of the parameter '{0}' are incompatible: Old API = {1}; " \
                      "New API = {2}".format(old_p['name'], old_p['dataType'], new_p['dataType']))
      new_params.remove(new_p)

  for new_p in new_params:
    if new_p.get('required'):
      errors.append("Required parameter '{0}' introduced in new API".format(new_p['name']))
  return errors

def find_parameter(params, key_param):
  """
  Find a parameter in params that matches key_param. If key_param is of type 'body', this
  method looks for a matching 'body' parameter among params. Otherwise, it matches up
  parameters by name.

  Args:
    params - A list of parameters
    key_param - A Swagger parameter description (dictionary)

  Returns:
    A parameter that matches key_param, or None if nothing found
  """
  if not params:
    return None
  if key_param['paramType'] == 'body':
    for param in params:
      if param['paramType'] == 'body':
        return param
    return None
  for param in params:
    if param['name'] == key_param['name']:
      return param
  return None

def compare_output_types(old_op, old_spec, new_op, new_spec):
  errors = []
  old_output = get_output_type(old_op)
  new_output = get_output_type(new_op)
  if old_output is not None and new_output is None:
    errors.append("New operation '{0}' has no return type".format(new_op['nickname']))
  elif old_output is None and new_output is not None:
    errors.append("New operation '{0}' has an output type while the corresponding old " \
             "operation doesn't".format(new_op['nickname']))
  elif old_output is not None and new_output is not None:
    old_output_type = swagger_to_eager_type(old_output, old_spec)
    new_output_type = swagger_to_eager_type(new_output, new_spec)
    compatible = is_output_compatible(old_output_type, new_output_type, errors)
    if not compatible:
      errors.append("Output types incompatible: Old operation = {0}; New operation = {1}".format(
        old_op['nickname'], new_op['nickname']))

  return errors

def get_output_type(operation):
  """
  Find the name of the output type of the specified operation. This method first explores
  the responseMessages defined for the operation. If a successful responseMessage (i.e.
  response code in 200's range), has been declared with a responseModel, that will be
  considered output type of the operation. Otherwise, it will fallback to the 'type'
  attribute defined in the operation. If neither is defined, returns None.

  Args:
    operation - A Swagger operation description (dictionary)

  Returns:
    A string representing a data type name or None
  """
  if operation.get('responseMessages'):
    for rm in operation['responseMessages']:
      if 200 <= rm['code'] < 210 and rm.get('responseModel'):
        return rm['responseModel']
  if operation.get('type'):
    return operation['type']
  return None

def find_operation(api, key_op):
  """
  Find an operation in api that matches key_op. This method first attempts to find a match
  by using the operation name (nickname). Failing that, it attempts to match up HTTP methods.

  Args:
    api - A Swagger API description (dictionary)
    key_op - A Swagger operation description (dictionary)

  Returns:
    An operation that matches key_op, or None if nothing found
  """
  operations = api['operations']
  for op in operations:
    if op['nickname'] == key_op['nickname']:
      return op

  for op in operations:
    if op['method'] == key_op['method']:
      return op
  return None

def is_related_operation(operation, ops):
  """
  Check whether the specified operation should be considered for API comparison. If no
  operations have been declared (i.e. ops is empty), all operations are considered for
  API comparison. Otherwise, only those operations that have been declared will be
  considered for comparison.

  Args:
    operation - Name of the operation that should be tested
    ops - List of declared operations

  Returns:
    True if the operation should be considered for API comparison, and False otherwise
  """
  if not ops:
    return True
  else:
    return operation in ops

def swagger_to_eager_type(data_type, spec, context=None):
  if data_type in PrimitiveType.PRIMITIVES:
    return PrimitiveType(data_type)

  if data_type == 'array':
    if not context:
      raise TypeCheckerException('Unrecognized container type without context')
    child_type = swagger_to_eager_type(context['items']['$ref'], spec)
    unique_items = context.get('uniqueItems') == True
    if unique_items:
      return ContainerType('set', child_type)
    else:
      return ContainerType('list', child_type)

  if spec.get('models'):
    for m_key,model in spec['models'].items():
      if model['id'] == data_type:
        fields = []
        for f_key, value in model['properties'].items():
          child_type_name = value['type']
          f = Field(f_key, swagger_to_eager_type(child_type_name, spec, value))
          if model.get('required') and f_key in model['required']:
            f.optional = False
          fields.append(f)
        return ComplexType(data_type, fields)

  raise TypeCheckerException('Unrecognized data type: {0}'.format(data_type))

