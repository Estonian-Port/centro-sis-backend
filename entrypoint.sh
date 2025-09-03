#!/bin/sh
set -e
python manage.py collectstatic --noinput
python manage.py makemigrations --noinput
python manage.py migrate --noinput

python - <<'PY'
import os, django
django.setup()
from django.contrib.auth import get_user_model
User = get_user_model()

u = os.environ.get("DJANGO_SUPERUSER_USERNAME")
p = os.environ.get("DJANGO_SUPERUSER_PASSWORD")

if u and p:
    if not User.objects.filter(**{User.USERNAME_FIELD: u}).exists():
        User.objects.create_superuser(**{User.USERNAME_FIELD: u}, password=p)
        print("Superuser created.")
    else:
        print("Superuser already exists; skipping.")
else:
    print("DJANGO_SUPERUSER_USERNAME/PASSWORD not set; skipping.")
PY

exec "$@"
