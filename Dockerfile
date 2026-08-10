# syntax=docker/dockerfile:1

# ---- Build stage ----
# Base build image
FROM openjdk:8u342 AS builder

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
RUN wget http://fpdownload.adobe.com/pub/flex/sdk/builds/flex3/flex_sdk_3.3.0.4852.zip && \
    mkdir -p /msoy/flex3 && \
    unzip flex_sdk_3.3.0.4852.zip -d /msoy/flex3 && \
    rm flex_sdk_3.3.0.4852.zip

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

# distcleanall wholesale-deletes dist/ (distclean's <delete dir="${deploy.dir}"/>)
# and pages/gwt (gclean's <delete dir="${gwtout.dir}"/>). Cache mounts below
# are only attached to the actual build RUN, so this has to run first, in an
# unmounted layer -- deleting a directory that's an active cache-mount
# mountpoint (or the parent of one) fails with "Unable to delete directory".
RUN if [ "$DEPLOYMENT" = "prod" ]; then ant distcleanall; fi

# dist/classes and dist/test-classes are cache-mounted so javac's incremental
# compilation carries over between `docker build` runs instead of starting
# from an empty directory every time (the prod path above still gets a fully
# clean tree first, since release builds should be reproducible). Both paths
# call `package` (not `distall`) so dist/packages/*.dpkg always gets produced
# for the final COPY below -- the dev/test build just has the test deployment
# config (e.g. msoy.localhost) baked in instead of prod's.
#
# pages/gwt is deliberately NOT cache-mounted -- GWT compilation isn't the
# slow part anyway (a few seconds), and mounting it would reintroduce the
# same "can't rmdir an active mountpoint" problem on the next prod build.
RUN --mount=type=cache,id=msoy-dist-classes,target=/msoy/dist/classes,sharing=locked \
    --mount=type=cache,id=msoy-dist-test-classes,target=/msoy/dist/test-classes,sharing=locked \
    if [ "$DEPLOYMENT" = "prod" ]; then \
        ant -Dflexsdk.dir=/msoy/flex3 -Dmaven.repo.remote=https://repo1.maven.org/maven2 \
            -Ddeployment=prod -Dmsoy.user=msoy -Dburl.user=msoy -Dmsoy.group=msoy package; \
    else \
        ant -Dflexsdk.dir=/msoy/flex3 -Dmaven.repo.remote=https://repo1.maven.org/maven2 \
            -Ddeployment=test -Ddev_deployment=$DEV_DEPLOYMENT -Dmsoy.user=msoy -Dburl.user=msoy -Dmsoy.group=msoy package; \
    fi


# ---- Output stage ----
FROM debian:bullseye-slim
COPY --from=builder /msoy/dist/packages/*.dpkg /packages/
