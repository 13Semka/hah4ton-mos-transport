"""
Application settings module.
"""
from dynaconf import Dynaconf, Validator

# Define validators for required settings
validators = [
    Validator('ENVIRONMENT', must_exist=True, is_type_of=str),
    Validator('DEBUG', must_exist=True, is_type_of=bool),
    Validator('API_V1_STR', must_exist=True, is_type_of=str),
    Validator('PROJECT_NAME', must_exist=True, is_type_of=str),
]

# Initialize configuration
settings = Dynaconf(
    envvar_prefix="APP",
    settings_files=['apiserver/config/settings.toml', 'apiserver/config/.secrets.toml'],
    environments=True,
    load_dotenv=True,
    validators=validators,
)

# Create aliases for convenience
settings.set('DEBUG', settings.get('debug', False))
settings.set('API_V1_STR', settings.get('api_v1_str', '/api/v1'))
settings.set('PROJECT_NAME', settings.get('project_name', 'API Server'))
settings.set('ENVIRONMENT', settings.get('environment', 'development')) 