package io.correcta.identities

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.EnableAspectJAutoProxy

@ConfigurationPropertiesScan
@EnableAspectJAutoProxy
@EnableFeignClients(basePackages = ["io.correcta.identities.clients"])
@SpringBootApplication
class LegalInquiriesApplication

fun main(args: Array<String>) {
  runApplication<LegalInquiriesApplication>(*args)
}