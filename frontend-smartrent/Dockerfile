# Etapa 1: Construcción de Angular
FROM node:22-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .
RUN NODE_OPTIONS="--max-old-space-size=2048" npm run build

# Etapa 2: Servidor Nginx ligero
FROM nginx:alpine
COPY --from=build /app/dist/frontend-smartrent/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
