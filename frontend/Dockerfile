# -- Stage 1: Build --
FROM node:20 AS builder
WORKDIR /app
COPY frontend/profdiff-web-application/package*.json ./
RUN npm ci
COPY frontend/profdiff-web-application/ ./
RUN npm run build

# -- Stage 2: Runtime --
FROM nginx:alpine
RUN rm -rf /usr/share/nginx/html/*
COPY --from=builder /app/dist/profdiff-web-application/browser /usr/share/nginx/html
COPY frontend/nginx.conf.template /etc/nginx/templates/default.conf.template
EXPOSE 80