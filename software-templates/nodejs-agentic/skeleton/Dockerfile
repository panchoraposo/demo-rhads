# Pipeline `build-source` already ran `npm ci` against Nexus into node_modules.
# Copy that tree instead of downloading again inside the image build.
FROM registry.access.redhat.com/ubi9/nodejs-18:1-108
WORKDIR /opt/app-root/src
COPY --chown=1001:0 package.json package-lock.json ./
COPY --chown=1001:0 node_modules ./node_modules
COPY --chown=1001:0 src ./src
EXPOSE 8080
CMD ["node", "src/index.js"]
