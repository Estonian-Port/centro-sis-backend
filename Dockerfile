# backend/Dockerfile
FROM python:3.12-slim

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1 \
    DJANGO_SETTINGS_MODULE=config.settings

WORKDIR /app

# Instalar dependencias del sistema (para psycopg2, Pillow, etc.)
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

# Instalar dependencias de Python
COPY requirements.txt .
RUN pip install --upgrade pip && \
    pip install -r requirements.txt && \
    pip install gunicorn psycopg2-binary

# Copiar código fuente
COPY . .

# Generar archivos estáticos
RUN python manage.py collectstatic --noinput

# Exponer puerto de la app
EXPOSE 8082

# Comando para producción
CMD ["gunicorn", "config.wsgi:application", "--bind", "0.0.0.0:8082"]
