#!/bin/bash
set -e

echo "===================================================="
echo "🚀 Iniciando instalación del Frontend SmartRent en EC2"
echo "===================================================="

# 1. Crear Swap de 2GB si no existe (evita bloqueos por falta de RAM al compilar)
if [ ! -f /swapfile ]; then
    echo "📦 Configurando 2GB de memoria Swap..."
    sudo fallocate -l 2G /swapfile
    sudo chmod 600 /swapfile
    sudo mkswap /swapfile
    sudo swapon /swapfile
    echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
else
    echo "✅ Memoria Swap ya configurada."
fi

# 2. Actualizar paquetes e instalar Nginx y utilidades
echo "🛠️ Instalando Nginx y dependencias del sistema..."
sudo apt-get update -y
sudo apt-get install -y nginx curl git

# 3. Instalar Node.js 22 LTS si no está instalado
if ! command -v node &> /dev/null; then
    echo "🟢 Instalando Node.js 22 LTS..."
    curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
    sudo apt-get install -y nodejs
else
    echo "✅ Node.js $(node -v) ya está instalado."
fi

# 4. Compilar el proyecto Angular
echo "⚙️ Instalando dependencias NPM y compilando Angular para producción..."
npm install
NODE_OPTIONS="--max-old-space-size=2048" npm run build

# 5. Publicar los archivos en Nginx
echo "🌐 Desplegando archivos estáticos en Nginx (/var/www/html)..."
sudo rm -rf /var/www/html/*
sudo cp -r dist/frontend-smartrent/browser/* /var/www/html/

# 6. Configurar Nginx para rutas SPA de Angular
echo "📝 Configurando servidor Nginx para SPA..."
sudo tee /etc/nginx/sites-available/default > /dev/null << 'EOF'
server {
    listen 80 default_server;
    listen [::]:80 default_server;

    root /var/www/html;
    index index.html;

    server_name _;

    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location ~* \.(jpg|jpeg|png|gif|ico|css|js|woff2)$ {
        expires 7d;
        add_header Cache-Control "public, no-transform";
    }
}
EOF

sudo nginx -t
sudo systemctl restart nginx
sudo systemctl enable nginx

echo "===================================================="
echo "✅ ¡Frontend SmartRent desplegado exitosamente!"
echo "🌐 IP Pública: http://$(curl -s ifconfig.me)"
echo "===================================================="
