FROM node:22-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

ARG VITE_API_BASE_URL=/api
ARG VITE_AUTH_USERNAME=admin
ARG VITE_AUTH_PASSWORD=admin1234
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_AUTH_USERNAME=$VITE_AUTH_USERNAME
ENV VITE_AUTH_PASSWORD=$VITE_AUTH_PASSWORD

RUN npm run build

FROM nginx:1.31-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
