import json
import os
from apimgt import swagger

try:
  from unittest import TestCase
except ImportError:
  from unittest.case import TestCase

class TestSwagger(TestCase):

  def load_file(self, file_name):
    current_dir = os.path.dirname(os.path.abspath(__file__))
    full_path = os.path.join(current_dir, 'samples', file_name)
    file_handle = open(full_path, 'r')
    spec = json.load(file_handle)
    file_handle.close()
    return spec

  def test_resource_path_incompatibility(self):
    api1 = self.load_file('1.json')
    api2 = self.load_file('2.json')
    status, message = swagger.is_api_compatible(api1, api2)
    self.assertFalse(status)

  def test_base_path_incompatibility(self):
    api1 = self.load_file('1.json')
    api2 = self.load_file('3.json')
    status, message = swagger.is_api_compatible(api1, api2)
    self.assertFalse(status)

  def test_missing_op_incompatibility(self):
    api1 = self.load_file('1.json')
    api2 = self.load_file('4.json')
    status, message = swagger.is_api_compatible(api1, api2)
    self.assertFalse(status)

  def test_method_compatibility(self):
    api1 = self.load_file('1.json')
    api2 = self.load_file('6.json')
    status, message = swagger.is_api_compatible(api1, api2)
    self.assertTrue(status)

  def test_http_method_incompatibility(self):
    api1 = self.load_file('1.json')
    api2 = self.load_file('5.json')
    status, message = swagger.is_api_compatible(api1, api2)
    self.assertFalse(status)

  def test_output_type_incompatibility_missing_field(self):
    api1 = self.load_file('7.json')
    api2 = self.load_file('8.json')
    status, message = swagger.is_api_compatible(api1, api2)
    self.assertFalse(status)

  def test_output_type_incompatibility_renamed_field(self):
    api1 = self.load_file('7.json')
    api2 = self.load_file('9.json')
    status, message = swagger.is_api_compatible(api1, api2)
    self.assertFalse(status)

  def test_output_type_incompatibility_different_type(self):
    api1 = self.load_file('7.json')
    api2 = self.load_file('10.json')
    status, message = swagger.is_api_compatible(api1, api2)
    self.assertFalse(status)

  def test_output_type_incompatibility_different_containers(self):
    api1 = self.load_file('13.json')
    api2 = self.load_file('14.json')
    status, message = swagger.is_api_compatible(api1, api2)
    self.assertFalse(status)

  def test_output_type_compatibility_different_type_name(self):
    api1 = self.load_file('7.json')
    api2 = self.load_file('11.json')
    status, message = swagger.is_api_compatible(api1, api2)
    self.assertTrue(status)

  def test_output_type_compatibility_additional_fields(self):
    api1 = self.load_file('7.json')
    api2 = self.load_file('12.json')
    status, message = swagger.is_api_compatible(api1, api2)
    self.assertTrue(status)

  def test_input_param_incompatibility_additional_fields(self):
    api1 = self.load_file('7.json')
    api2 = self.load_file('15.json')
    status, message = swagger.is_api_compatible(api1, api2)
    self.assertFalse(status)

  def test_input_param_incompatibility_invalid_data_type(self):
    api1 = self.load_file('7.json')
    api2 = self.load_file('16.json')
    status, message = swagger.is_api_compatible(api1, api2)
    self.assertFalse(status)

  def test_input_param_incompatibility_invalid_param_type(self):
    api1 = self.load_file('7.json')
    api2 = self.load_file('17.json')
    status, message = swagger.is_api_compatible(api1, api2)
    self.assertFalse(status)

  def test_input_param_incompatibility_optional_param_made_required(self):
    api1 = self.load_file('18.json')
    api2 = self.load_file('15.json')
    status, message = swagger.is_api_compatible(api1, api2)
    self.assertFalse(status)

  def test_input_param_incompatibility_additional_required_field(self):
    api1 = self.load_file('19.json')
    api2 = self.load_file('20.json')
    status, message = swagger.is_api_compatible(api1, api2)
    self.assertFalse(status)

  def test_input_param_compatibility_additional_optional_param(self):
    api1 = self.load_file('15.json')
    api2 = self.load_file('18.json')
    status, message = swagger.is_api_compatible(api1, api2)
    self.assertTrue(status)

  def test_input_param_compatibility_additional_optional_field(self):
    api1 = self.load_file('19.json')
    api2 = self.load_file('21.json')
    status, message = swagger.is_api_compatible(api1, api2)
    self.assertTrue(status)

