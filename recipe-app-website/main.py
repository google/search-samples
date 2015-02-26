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

import jinja2
import json
import os
import webapp2

JINJA_ENVIRONMENT = jinja2.Environment(
    loader=jinja2.FileSystemLoader(os.path.join(os.path.dirname(__file__), 'templates')),
    extensions=['jinja2.ext.autoescape'],
    autoescape=True)

def load_recipe(recipe_id):
    filename = os.path.dirname(__file__) + '/recipes/' + recipe_id + '.json'
    recipe = json.loads(open(filename, 'r').read())
    recipe['id'] = recipe_id
    return recipe

class MainPage(webapp2.RequestHandler):
    def get(self):
        template_values = {
            'title': 'RecipeApp'
        }
        template = JINJA_ENVIRONMENT.get_template('index.html')
        self.response.write(template.render(template_values))

class RecipePage(webapp2.RequestHandler):
    def get(self, recipe_id):
        query = self.request.get('q')
        num_results = 0
        if query:
            num_results = 1
    	recipe = load_recipe(recipe_id)

        ingredient_sections = ['']
        ingredients_by_section = {'':[]}
        for ingredient in recipe['ingredients']:
            if 'category' in ingredient:
                category = ingredient['category']
                ingredient_section = []
                if not category in ingredients_by_section:
                    ingredients_by_section[category] = ingredient_section
                    ingredient_sections.append(category)
                else:
                    ingredient_section = ingredients_by_section[category]
                ingredient_section.append(ingredient)
            else:
                ingredients_by_section[''].append(ingredient)
        
        template_values = {
            'title': recipe['title'],
            'recipe': recipe,
            'ingredients': ingredients_by_section,
            'ingredient_sections': ingredient_sections,
            'query': query,
            'num_results': num_results
        }
        template = JINJA_ENVIRONMENT.get_template('recipe.html')
        self.response.write(template.render(template_values))

class SearchResultsPage(webapp2.RequestHandler):
    def get(self):
        query = self.request.get('q')
        results = []
        clean_query = query.lower().strip()
        if clean_query.endswith('recipes'):
            clean_query = clean_query[:-7].strip()
        for recipe_id in ['grilled-potato-salad', 'haloumi-salad', 'pierogi-poutine', 'wedge-salad', 'malaga-paella']:
            recipe = load_recipe(recipe_id)
            if recipe['title'].lower().find(clean_query) >= 0:
                results.append(recipe)

        if len(results) == 1:
            self.redirect('/recipe/' + results[0]['id'] + '?q=' + query)
        else:
            template_values = {
                'title': '"' + query + '" - RecipeApp',
                'query': query,
                'results': results,
                'num_results': len(results)
            }
            template = JINJA_ENVIRONMENT.get_template('search.html')
            self.response.write(template.render(template_values))

application = webapp2.WSGIApplication([
    ('/', MainPage),
    (r'/recipe/(.+)', RecipePage),
    (r'/search', SearchResultsPage)
], debug=True)