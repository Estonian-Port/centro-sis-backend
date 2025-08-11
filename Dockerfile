FROM python:3.12-slim
ENV PYTHONUNBUFFERED=1 PYTHONDONTWRITEBYTECODE=1 PIP_NO_CACHE_DIR=1 DJANGO_SETTINGS_MODULE=config.settings
WORKDIR /app

# Forzar IPv4 para evitar el error de red
RUN apt-get update -o Acquire::ForceIPv4=true \
 && apt-get install -y --no-install-recommends build-essential libpq-dev \
 && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --upgrade pip \
 && pip install --no-cache-dir -r requirements.txt \
 && pip install --no-cache-dir gunicorn psycopg2-binary

COPY . .
RUN python manage.py collectstatic --noinput

EXPOSE 8082
CMD ["gunicorn", "config.wsgi:application", "--bind", "0.0.0.0:8082"]
