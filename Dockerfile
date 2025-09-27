# ---- Build stage ----
# Base build image
FROM openjdk:8-jdk as builder

# Install dependencies
RUN apt-get update && \
    apt-get install -y patch maven ant postgresql-client wget unzip curl git && \
    apt-get clean

# Download and install Jetty parent pom
RUN curl -L -o jetty-parent-17.pom https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-parent/17/jetty-parent-17.pom && \
    mvn install:install-file \
        -Dfile=jetty-parent-17.pom \
        -DgroupId=org.eclipse.jetty \
        -DartifactId=jetty-parent \
        -Dversion=17 \
        -Dpackaging=pom && \
    rm jetty-parent-17.pom

# Set working directory
WORKDIR /msoy

# Download and extract Flex SDK
RUN wget http://fpdownload.adobe.com/pub/flex/sdk/builds/flex3/flex_sdk_3.3.0.4852A.zip && \
    mkdir -p /msoy/flex3 && \
    unzip flex_sdk_3.3.0.4852A.zip -d /msoy/flex3 && \
    rm flex_sdk_3.3.0.4852A.zip

# Copy host repo into container
COPY . /msoy

# Build the project
COPY m2/settings.xml /root/.m2/settings.xml

RUN mkdir -p /root/.m2 && \
    cd /root/ && \
    jar xf /msoy/artifacts.jar

RUN wget -O/msoy/lib/gwt-asyncgen.jar https://repo1.maven.org/maven2/com/samskivert/gwt-asyncgen/1.0/gwt-asyncgen-1.0.jar

# Build arguments for deployment type
ARG DEPLOYMENT=prod
ARG DEV_DEPLOYMENT=false

# Build the project
RUN ant distcleanall && \
    if [ "$DEPLOYMENT" = "prod" ]; then \
        ant -Dflexsdk.dir=/msoy/flex3 -Dmaven.repo.remote=https://repo1.maven.org/maven2 \
            -Ddeployment=prod -Dmsoy.user=msoy -Dburl.user=msoy -Dmsoy.group=msoy package; \
    else \
        ant -Dflexsdk.dir=/msoy/flex3 -Dmaven.repo.remote=https://repo1.maven.org/maven2 \
            -Ddeployment=test -Ddev_deployment=$DEV_DEPLOYMENT -Dmsoy.user=msoy -Dburl.user=msoy -Dmsoy.group=msoy distall; \
    fi


# ---- Output stage ----
FROM debian:bullseye-slim
COPY --from=builder /msoy/dist/packages/*.deb /packages/