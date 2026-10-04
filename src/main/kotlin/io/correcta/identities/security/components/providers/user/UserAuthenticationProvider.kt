package io.correcta.identities.security.components.providers.user

import io.correcta.identities.config.properties.JsonWebTokenProperties
import io.correcta.identities.entities.InstitutionalProfileType
import io.correcta.identities.security.components.providers.user.models.UserPrincipal
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component

@Component
class UserAuthenticationProvider(
    jsonWebTokenProperties: JsonWebTokenProperties,
) {

    private val signingKey =
        Keys.hmacShaKeyFor(jsonWebTokenProperties.secret.toByteArray())

    fun getAuthentication(token: String): UsernamePasswordAuthenticationToken {
        val claims = Jwts.parserBuilder()
            .setSigningKey(signingKey)
            .build()
            .parseClaimsJws(token)
            .body

        val principal = UserPrincipal(
            profileId = claims.subject,
            sessionId = claims["sid", String::class.java],
            type = claims["profile", String::class.java].let { InstitutionalProfileType.valueOf(it) }
        )

        return UsernamePasswordAuthenticationToken(principal, null, emptyList())
    }
}