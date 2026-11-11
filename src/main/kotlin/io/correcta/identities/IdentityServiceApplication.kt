package io.correcta.identities

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.EnableAspectJAutoProxy

/** Spring Boot entry point of the identity service. */
@ConfigurationPropertiesScan
@EnableAspectJAutoProxy
@EnableFeignClients(basePackages = ["io.correcta.identities.clients"])
@SpringBootApplication
class IdentityServiceApplication

/** Starts the identity service. */
fun main(args: Array<String>) {
  runApplication<IdentityServiceApplication>(*args)
}