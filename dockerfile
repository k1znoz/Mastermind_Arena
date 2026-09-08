FROM eclipse-temurin:21-jdk
WORKDIR /app

COPY . .
RUN apt-get update && apt-get install -y maven

EXPOSE 8080
ENV APP_HTTP_HOST=0.0.0.0
ENV APP_HTTP_PORT=8080

CMD ["mvn","-q","-pl","deduction-engine","compile","exec:java","-Dexec.mainClass=io.mastermindarena.deduction.api.submitaction.LocalSubmitActionHttpServerMain"]