package io.correcta.identities.clients

import io.correcta.identities.models.response.clients.ProxyCheckResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@FeignClient(
    name = "proxyCheckClient",
    url = "\${clients.proxy-check.url}"
)
interface ProxyCheckClient {

    @GetMapping("/v3/{ip}")
    fun checkIp(
        @PathVariable ip: String
    ): ProxyCheckResponse

}