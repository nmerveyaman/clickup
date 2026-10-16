# 1. Aşama: Build (Vite projesini derleme)
FROM node:22-alpine AS build

WORKDIR /app

# Bağımlılıkları önbelleğe almak (cache) için package dosyalarını kopyala
COPY package*.json ./

RUN npm install

# Tüm kaynak kodları kopyala ve Vite production build al
COPY . .
ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

# 2. Aşama: Production Yayınlama (Nginx Web Server)
FROM nginx:alpine-slim

# Vite varsayılan olarak çıktıları 'dist' klasörüne üretir
COPY --from=build /app/dist /usr/share/nginx/html

# SPA (React Router vb.) yönlendirme desteği için varsayılan Nginx konfigürasyonu
RUN echo $'server {\n\
    listen 80;\n\
    location / {\n\
        root /usr/share/nginx/html;\n\
        index index.html index.htm;\n\
        try_files $uri $uri/ /index.html;\n\
    }\n\
}' > /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]