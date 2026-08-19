# 运行镜像：只装JRE+jar，体积小启动快。
# jar由start.ps1先mvn package好再构建（复用本机Maven和镜像源，比容器里下载依赖快得多）
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/kb-service-1.0.0.jar app.jar
EXPOSE 8082
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
