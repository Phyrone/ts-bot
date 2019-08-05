#FROM openjdk:8-slim
FROM shipilev/openjdk-shenandoah:11
MAINTAINER Phyrone<phyrone@phyrone.de>
COPY . /build/
RUN cd /build/ && chmod a+x gradlew && ./gradlew --no-daemon clean shadowJar
RUN ls /build/build/libs/ && mkdir /opt/bot/
RUN cp /build/build/libs/mini-ts-bot.jar /opt/bot/mini-ts-bot.jar && rm -R /build/
VOLUME /opt/bot/
WORKDIR /app/
VOLUME /app/
CMD java -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -Dfile.encoding=UTF-8 -Xms50m -Xmx50m -jar /opt/bot/mini-ts-bot.jar
