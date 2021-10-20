
FROM openjdk:18-slim as build
COPY . /build/
RUN cd /build/ && chmod a+x gradlew && ./gradlew --no-daemon clean shadowJar
#RUN cp /build/build/libs/mini-ts-bot.jar /opt/bot/mini-ts-bot.jar && rm -R /build/
FROM openjdk:11-jre-slim
#FROM shipilev/openjdk-shenandoah:11
MAINTAINER Phyrone<phyrone@phyrone.de>
COPY --from=build /build/build/libs/mini-ts-bot.jar /usr/lib/mini-ts-bot.jar
USER 600
VOLUME /opt/bot/
WORKDIR /app/
VOLUME /app/
CMD java  -XX:MaxGCPauseMillis=10 -XX:+UseG1GC -XX:+UseStringDeduplication -Dfile.encoding=UTF-8 -Xms50m -Xmx50m -jar /usr/lib/mini-ts-bot.jar
