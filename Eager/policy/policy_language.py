import ast
import _ast

class EagerPolicyLanguageException(Exception):
  pass

class EagerPolicyParser(ast.NodeVisitor):

  # Disallowed built-in functions:
  #   basestring(), classmethod(), compile(), dir(), eval(), execfile(), file()
  #   globals(), help(), input(), locals(), open(), print(), property(), raw_input()
  #   reload(), staticmethod()

  FUNCTION_WHITE_LIST = (
    'assert_dependency', 'assert_dependency_in_range', 'assert_not_dependency',
    'assert_true', 'assert_false',
    'abs', 'all', 'any', 'bin', 'bool', 'bytearray', 'callable', 'chr', 'cmp',
    'complex', 'delattr', 'dict', 'divmod', 'enumerate', 'filter', 'float', 'format',
    'frozenset', 'getattr', 'hasattr', 'hash', 'hex', 'id', 'int', 'isinstance',
    'issubclass', 'iter', 'len', 'list', 'long', 'map', 'max', 'memoryview', 'min',
    'next', 'object', 'oct', 'ord', 'pow', 'range', 'reduce', 'repr', 'reversed',
    'round', 'set', 'setattr', 'slice', 'sorted', 'str', 'sum', 'super', 'tuple',
    'type', 'unichr', 'unicode', 'vars', 'xrange', 'zip'
  )

  MODULE_WHITE_LIST = (
    're'
  )

  def parse(self, source_code):
    self.defined_functions = []
    self.called_functions = []
    self.visit(self.__source_to_ast(source_code))
    for f in self.called_functions:
      if f not in self.defined_functions and f not in self.FUNCTION_WHITE_LIST:
        raise EagerPolicyLanguageException('Call to undefined function {0}'.format(f))

  def get_description(self, source_code):
    return ast.get_docstring(self.__source_to_ast(source_code))

  def __source_to_ast(self, source_code):
    return ast.parse(source_code, mode='exec')

  def visit(self, node):
    if isinstance(node, _ast.Global):
      raise EagerPolicyLanguageException('Keyword global is not allowed')
    elif isinstance(node, _ast.Call):
      if isinstance(node.func, _ast.Name):
        self.called_functions.append(node.func.id)
    elif isinstance(node, _ast.Import):
      for name in node.names:
        if name.name not in self.MODULE_WHITE_LIST:
          raise EagerPolicyLanguageException('Module {0} is not supported'.format(name.name))
    elif isinstance(node, _ast.ImportFrom):
      if node.module not in self.MODULE_WHITE_LIST:
        raise EagerPolicyLanguageException('Module {0} is not supported'.format(node.module))
      for name in node.names:
        self.defined_functions.append(name.name)
    elif isinstance(node, _ast.ClassDef):
      raise EagerPolicyLanguageException('Class definitions are not allowed')
    elif isinstance(node, _ast.FunctionDef):
      self.defined_functions.append(node.name)
    elif isinstance(node, _ast.Print):
      raise EagerPolicyLanguageException('print statements are not allowed')
    #print node
    self.generic_visit(node)

def validate_policy(code_str):
  policy_parser = EagerPolicyParser()
  policy_parser.parse(code_str)

def get_policy_description(code_str):
  policy_parser = EagerPolicyParser()
  return policy_parser.get_description(code_str)
