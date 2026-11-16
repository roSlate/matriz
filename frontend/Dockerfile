# syntax=docker/dockerfile:1
# Frontend image: Node builds the static files, then nginx serves them and forwards /api to the backend.
# No credentials are in this file.

FROM node:24-alpine AS build
WORKDIR /build
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY . .
RUN npm run build

# The unprivileged nginx image runs as a normal user and listens on 8080.
FROM nginxinc/nginx-unprivileged:stable-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html
EXPOSE 8080
