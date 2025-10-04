FROM openjdk:8-jre-slim

FROM openjdk:8-jdk AS builder
# Install dependencies
RUN apt-get update && apt-get install -y \
    postgresql-client \
    dpkg \
    mailutils \
    ssmtp \
    nano \
    && rm -rf /var/lib/apt/lists/*

RUN touch /root/.mail /root/.mailrc && chmod 777 /root /root/.mail /root/.mailrc

RUN useradd -m -s /bin/bash msoy
RUN useradd -m -s /bin/bash burl
RUN usermod -aG msoy burl

COPY start.sh /start.sh
COPY entrypoint.sh /entrypoint.sh

RUN chmod +x /start.sh

# Copy your built .dpkg packages into the container
COPY ./docker-staging/*.dpkg /tmp/

# Install the packages
RUN dpkg -i /tmp/msoy-server-code_*.dpkg && \
    dpkg -i /tmp/msoy-server_*.dpkg && \
    dpkg -i /tmp/burl-server_*.dpkg

RUN rm /tmp/*.dpkg

RUN chown -R msoy:msoy /export/msoy
RUN chmod -R +770 /export/msoy
