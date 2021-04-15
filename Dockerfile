FROM anapsix/alpine-java:8_jdk

# Install Gradle
RUN apk add --update openssl \
	&& wget https://services.gradle.org/distributions/gradle-6.5-bin.zip \
	&& unzip gradle-6.5-bin.zip -d /opt/ \
	&& mv opt/gradle-6.5/ opt/gradle \
	&& rm gradle-6.5-bin.zip

ENV GRADLE /opt/gradle/bin
ENV PATH ${PATH}:${GRADLE}

ADD gradle.properties /.gradle/
ENV GRADLE_USER_HOME /.gradle/

# Get SDK Platform Tools
RUN mkdir /opt/android-sdk
ENV ANDROID_HOME /opt/android-sdk

RUN wget https://dl.google.com/android/repository/sdk-tools-linux-3859397.zip \
	&& unzip sdk-tools-linux-3859397.zip -d ${ANDROID_HOME} \
	&& rm sdk-tools-linux-3859397.zip

ENV PATH ${PATH}:${ANDROID_HOME}/tools/bin

RUN yes | sdkmanager --licenses \
	&& sdkmanager "platforms;android-29" "platform-tools" "extras;google;m2repository" "build-tools;29.0.0" --verbose

# bash_r is a wrapper for bash that removes \r
ADD bash_r.tgz /bin