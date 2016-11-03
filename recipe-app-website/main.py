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

def load_recipe_names():
    all_recipe_ids = []
    for file in os.listdir('recipes'):
        if file.endswith('.json'):
            # strip off the .json extension first
            all_recipe_ids.append(file.rsplit('.', 1)[0])
    return sorted(all_recipe_ids)

# TODO(zshahzad): Make website mobile-friendly/responsive so
# AMP/non-AMP pages can be consolidated into one recipe.html
# template and this isn't needed
def recipe_template_for_url(recipe_id):
        if recipe_id.startswith('amp/'):
            recipe_id = recipe_id.rsplit('amp/', 1)[1]
            if recipe_id:
                return 'amp-recipe.html', recipe_id
        return 'recipe.html', recipe_id

class MainPage(webapp2.RequestHandler):
    def get(self):
        template_values = {
            'title': 'RecipeApp'
        }
        template = JINJA_ENVIRONMENT.get_template('index.html')
        self.response.write(template.render(template_values))

class AllRecipesPage(webapp2.RequestHandler):
    def get(self):
        available_recipes = load_recipe_names()
        results = []

        for recipe_id in available_recipes:
            recipe = load_recipe(recipe_id)
            results.append(recipe)

        template_values = {
            'title': 'Recipes Listing - RecipeApp',
            'results': results,
            'num_results': len(results)
        }
        template = JINJA_ENVIRONMENT.get_template('recipes.html')
        self.response.write(template.render(template_values))

class RecipePage(webapp2.RequestHandler):
    def get(self, recipe_id):
        query = self.request.get('q')
        num_results = 0
        if query:
            num_results = 1

        template_url, recipe_id = recipe_template_for_url(recipe_id)
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
        template = JINJA_ENVIRONMENT.get_template(template_url)
        self.response.write(template.render(template_values))

class SearchResultsPage(webapp2.RequestHandler):
    def get(self):
        query = self.request.get('q')
        available_recipes = load_recipe_names()
        results = []
        clean_query = query.lower().strip()
        if clean_query.endswith('recipes'):
            clean_query = clean_query[:-7].strip()
        for recipe_id in available_recipes:
            recipe = load_recipe(recipe_id)
            recipe_ingredients = recipe['ingredients']
            if any(clean_query in ingred['name'].lower() for ingred in recipe_ingredients):
                results.append(recipe)
            elif recipe['title'].lower().find(clean_query) >= 0:
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
    (r'/allrecipes', AllRecipesPage),
    (r'/recipe/(.+)', RecipePage),
    (r'/search', SearchResultsPage)
], debug=True)
