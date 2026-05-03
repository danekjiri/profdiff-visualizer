# -- Stage 1: Build --
FROM ghcr.io/graalvm/jdk-community:21 AS builder
COPY --from=maven:3.9.9 /usr/share/maven /usr/share/maven
ENV PATH="/usr/share/maven/bin:${PATH}"
WORKDIR /build
COPY resources/ resources/
RUN chmod +x resources/scripts_benchmarks/mvn_install_file.sh && \
    ./resources/scripts_benchmarks/mvn_install_file.sh resources/profdiff-dependency-jars
COPY backend/ backend/
RUN cd backend && mvn clean package -DskipTests

# -- Stage 2: Runtime --
FROM ghcr.io/graalvm/jdk-community:21
WORKDIR /app
COPY --from=builder /build/backend/profdiff-api/target/*.jar app.jar
COPY resources/default-benchmarks /default-benchmarks
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]