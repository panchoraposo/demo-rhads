FROM registry.access.redhat.com/ubi9/nodejs-22:latest
WORKDIR /opt/app-root/src
COPY package.json ./
RUN npm install --omit=dev
COPY src ./src
EXPOSE 8080
CMD ["node", "src/index.js"]
