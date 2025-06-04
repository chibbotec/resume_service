from utils.shortcuts import get_env

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.postgresql_psycopg2',
        'HOST': get_env("POSTGRES_HOST", "oj-postgres"),
        'PORT': get_env("POSTGRES_PORT", "5432"),
        'NAME': get_env("POSTGRES_DB"),
        'USER': get_env("POSTGRES_USER"),
        'PASSWORD': get_env("POSTGRES_PASSWORD")
    }
}

REDIS_CONF = {
    "host": get_env("REDIS_HOST", "oj-redis"),
    "port": get_env("REDIS_PORT", "6379")
}

JUDGE_SERVER_TOKEN = get_env("JUDGE_SERVER_TOKEN", "jdkeoakkxkk34kdak")

SPACE_SERVICE_URL = get_env("SPACE_SERVICE_URL", "http://172.30.1.23:9030")

DEBUG = True

ALLOWED_HOSTS = ['*']

DATA_DIR = "/data"
