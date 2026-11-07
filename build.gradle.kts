import com.google.protobuf.gradle.id
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.4.12"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "8.10.4"
    id("org.hibernate.orm") version "6.6.33.Final"
    id("com.google.protobuf") version "0.9.4"
    kotlin("plugin.jpa") version "1.9.25"
    kotlin("kapt") version "1.9.25"
    kotlin("plugin.lombok") version "1.9.25"
}

group = "io.correcta"
version = "0.0.1-SNAPSHOT"
description = "identity-service"

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

extra["springCloudVersion"] = "2024.0.0"
extra["springGrpcVersion"] = "0.12.0"

dependencies {
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.14")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // gRPC core
    implementation("io.grpc:grpc-netty:1.75.0")
    implementation("io.grpc:grpc-protobuf:1.65.0")
    implementation("io.grpc:grpc-stub:1.65.0")

    // Needed for Java 17+ (Spring Boot 3) — gRPC uses javax
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // JsonWebToken
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    // Yauaa
    implementation("nl.basjes.parse.useragent:yauaa:8.2.0")

    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    runtimeOnly("org.postgresql:postgresql")

    kapt("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.grpc:spring-grpc-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("com.h2database:h2")
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.grpc:spring-grpc-dependencies:${property("springGrpcVersion")}")
    }

    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

hibernate {
    enhancement {
        enableAssociationManagement = true
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.3"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.65.0"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("grpc") {
                    option("@generated=omit")
                }
            }
        }
    }
}

sourceSets {
    named("main") {
        java.srcDir("build/generated/source/proto/main/java")
        java.srcDir("build/generated/source/proto/main/grpc")
    }
}


allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<BootBuildImage> {
    environment.put("JAVA_TOOL_OPTIONS", "-Dhttps.protocols=TLSv1.2,TLSv1.3")
    environment.put("BP_PACKAGES", "ca-certificates")

    if (System.getProperty("os.arch") == "aarch64") {
        environment.put("BP_JVM_VERSION", "17")

        environment.put("BP_OCI_SOURCE", "")
        environment.put("BP_ENABLE_SBOM", "false")
        environment.put("BP_LOG_LEVEL", "DEBUG")
    }

    // Only needed for GraalVM native builds
    environment.put(
        "BP_NATIVE_IMAGE_BUILD_ARGUMENTS",
        "--enable-all-security-services --initialize-at-run-time=org.slf4j,ch.qos.logback,org.apache.logging --report-unsupported-elements-at-runtime"
    )
    environment.put("BP_NATIVE_IMAGE_CONFIG_DIR", "src/main/resources/META-INF/native-image")

    if (System.getProperty("os.arch") == "aarch64")
        imagePlatform.set("linux/arm64")
}
kapt {
    keepJavacAnnotationProcessors = true
}

val ktlintOverrides =
    mapOf(
        "ktlint_standard_no-wildcard-imports" to "disabled",
        "ktlint_standard_max-line-length" to "disabled",
        "ktlint_standard_backing-property-naming" to "disabled",
        "ktlint_standard_property-naming" to "disabled",
        "ktlint_standard_function-naming" to "disabled",
        "ktlint_standard_filename" to "disabled",
        "ktlint_standard_package-name" to "disabled",
    )

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint("1.7.1").editorConfigOverride(ktlintOverrides)
    }

    kotlinGradle {
        target("*.gradle.kts")
        ktlint("1.7.1").editorConfigOverride(ktlintOverrides)
    }
}

val installGitHooks by tasks.registering(Exec::class) {
    commandLine("git", "config", "core.hooksPath", ".githooks")
    isIgnoreExitValue = true
}

tasks.named("compileKotlin") {
    dependsOn(installGitHooks)
}
