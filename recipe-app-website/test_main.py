# Copyright 2014 Google Inc. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import unittest
import mock
import main

# to run tests, type `python test_main.py` on command line
class TestMain(unittest.TestCase):

    @mock.patch('os.listdir')
    def test_load_recipe_names(self, mock_listdir):
        # test for a combination of json and txt files.
        # only json files should be returned by the load_recipe_names method
        mock_listdir.return_value = ['b.json', 'c.txt', 'a.json']
        files = main.load_recipe_names()
        # check only 2 file names returned
        self.assertEqual(2, len(files))
        # check they files are also in alphabetical order
        self.assertEqual(files, ['a', 'b'])

        # empty directory should return empty list
        mock_listdir.return_value = []
        files = main.load_recipe_names()
        self.assertEqual(0, len(files))

        # directory of only txt files should return empty list
        mock_listdir.return_value = ['a.txt', 'b.txt']
        files = main.load_recipe_names()
        self.assertEqual(0, len(files))

    def test_recipe_template_for_url(self):
        url = 'amp/'
        self.assertEqual(main.recipe_template_for_url(url), ('recipe.html', ''))

        # amp template requested
        url = 'amp/soup-recipe-id'
        self.assertEqual(main.recipe_template_for_url(url), ('amp-recipe.html', 'soup-recipe-id'))

        # regular template requested
        url = 'soup-recipe-id'
        self.assertEqual(main.recipe_template_for_url(url), ('recipe.html', 'soup-recipe-id'))

if __name__ == '__main__':
    unittest.main()
