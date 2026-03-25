plugins {
	java
	id("org.springframework.boot") version "3.5.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.briefin"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	compileOnly("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	runtimeOnly("com.h2database:h2")
	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")

	implementation ("org.springframework.boot:spring-boot-starter-webflux")

	implementation("io.jsonwebtoken:jjwt-api:0.11.5")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")
	implementation("me.paulschwarz:spring-dotenv:2.5.4")

	implementation("org.jsoup:jsoup:1.17.2")
	implementation("com.pgvector:pgvector:0.1.6")
	implementation("org.hibernate.orm:hibernate-vector:6.6.13.Final")

	implementation("nl.martijndwars:web-push:5.1.1")
	implementation("org.bouncycastle:bcprov-jdk15on:1.70")

	implementation("org.springframework.boot:spring-boot-starter-websocket")

	implementation("io.netty:netty-resolver-dns-native-macos:4.1.101.Final:osx-aarch_64")

	implementation ("org.springframework.boot:spring-boot-starter-data-redis")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
