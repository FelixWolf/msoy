# Dockerfile
FROM openjdk:8-jdk

# Install ant and other dependencies
RUN apt-get update && \
    apt-get install -y patch maven ant postgresql-client && \
    apt-get clean

# Create a working directory
WORKDIR /msoy

# Clone the repo and build
RUN git clone https://github.com/felixwolf/msoy.git /msoy
RUN git clone https://github.com/felixwolf/thane.git /msoy/projects/thane

# Download and install Jetty
RUN curl -L -o jetty-parent-17.pom https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-parent/17/jetty-parent-17.pom && \
    mvn install:install-file \
        -Dfile=jetty-parent-17.pom \
        -DgroupId=org.eclipse.jetty \
        -DartifactId=jetty-parent \
        -Dversion=17 \
        -Dpackaging=pom && \
    rm jetty-parent-17.pom

# Download and install Flex SDK
RUN wget http://fpdownload.adobe.com/pub/flex/sdk/builds/flex3/flex_sdk_3.3.0.4852A.zip \
 && mkdir -p /msoy/flex3 \
 && unzip flex_sdk_3.3.0.4852A.zip -d /msoy/flex3

COPY config/ /msoy/etc/test/

# Fix the outdated Maven repo URL
RUN sed -i 's|http://repo1.maven.org/|https://repo1.maven.org/|g' /msoy/projects/thane/build.xml

# Build the project
COPY m2/settings.xml /root/.m2/settings.xml

COPY artifacts.jar /tmp/artifacts.jar
RUN mkdir -p /root/.m2 && \
    cd /root/ && \
    jar xf /tmp/artifacts.jar && \
    rm /tmp/artifacts.jar

COPY msoy-pom.xml.patch /tmp/msoy-pom.xml.patch
RUN patch /msoy/pom.xml < /tmp/msoy-pom.xml.patch && rm /tmp/msoy-pom.xml.patch
COPY MsoyServer.java.patch /tmp/MsoyServer.java.patch
RUN patch /msoy/src/java/com/threerings/msoy/server/MsoyServer.java < /tmp/MsoyServer.java.patch && rm /tmp/MsoyServer.java.patch

RUN ant distcleanall
RUN ant -Dflexsdk.dir=/msoy/flex3 -Dmaven.repo.remote=https://repo1.maven.org/maven2 -Dsource 1.6 -Dtarget 1.6 distall
RUN ant compile
RUN ant dist
RUN ant flashapps
RUN ant gclients
RUN ant genasync
RUN ant tests
RUN ant thane-client
RUN ant viewer

# Expose the ports used by the server
EXPOSE 8080 4010 47623

# Start the server
CMD ["./bin/msoyserver"]