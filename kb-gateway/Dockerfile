# 网关运行镜像：jar由start.ps1预先mvn package
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/kb-gateway-1.0.0.jar app.jar
EXPOSE 9001
ENV JAVA_OPTS="-Xms128m -Xmx256m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
