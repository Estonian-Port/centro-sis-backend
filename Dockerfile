FROM python:3.12-slim

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1 \
    DJANGO_SETTINGS_MODULE=config.settings

WORKDIR /app

# Depedencias Python (usar wheels binarios)
COPY requirements.txt .
RUN pip install --upgrade pip \
 && pip install --no-cache-dir -r requirements.txt \
 && pip install --no-cache-dir gunicorn psycopg2-binary

# Código
COPY . .

# Static
RUN python manage.py collectstatic --noinput

EXPOSE 8082
CMD ["gunicorn", "config.wsgi:application", "--bind", "0.0.0.0:8082"]
