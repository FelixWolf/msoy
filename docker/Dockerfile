FROM openjdk:8-jre-slim

# Install dependencies
RUN apt-get update && apt-get install -y \
    postgresql-client \
    dpkg \
    mailutils \
    ssmtp \
    nano \
    && rm -rf /var/lib/apt/lists/*

# Copy your built .dpkg packages into the container
COPY docker-staging/*.dpkg /tmp/

# Install the packages
RUN dpkg -i /tmp/msoy-server-code_*.dpkg && \
    dpkg -i /tmp/msoy-server_*.dpkg && \
    dpkg -i /tmp/burl-server_*.dpkg

RUN useradd -m -s /bin/bash msoy
RUN useradd -m -s /bin/bash burl
RUN usermod -aG msoy burl
RUN chown -R msoy:msoy /export/msoy
RUN chmod -R +770 /export/msoy

RUN touch /root/.mail /root/.mailrc && chmod 777 /root /root/.mail /root/.mailrc

RUN echo -e '#!/bin/bash\necho "Pretending to send mail to: $@"\nexit 0' > /usr/bin/mail && chmod +x /usr/bin/mail

COPY start.sh /start.sh
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /start.sh