import ast
import _ast

class EagerPolicyLanguageException(Exception):
  pass

class EagerPolicyParser(ast.NodeVisitor):
  FUNCTION_WHITE_LIST = (
    'assert_dependency', 'assert_dependency_in_range', 'assert_not_dependency',
    'assert_true', 'assert_false', 'len', 'range', 'pow', 'all', 'any'
  )

  MODULE_WHITE_LIST = (
    're'
  )

  def parse_policy(self, code):
    self.defined_functions = []
    self.called_functions = []
    self.visit(code)
    for f in self.called_functions:
      if f not in self.defined_functions and f not in self.FUNCTION_WHITE_LIST:
        raise EagerPolicyLanguageException('Call to undefined function {0}'.format(f))

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

def validate_policy_content(code_ast):
  policy_parser = EagerPolicyParser()
  policy_parser.parse_policy(code_ast)

def validate_policy(code_str):
  code_ast = ast.parse(code_str, mode='exec')
  validate_policy_content(code_ast)