package io.correcta.identities.controllers

import io.correcta.identities.models.request.LoginUserRequest
import io.correcta.identities.models.request.RefreshTokenRequest
import io.correcta.identities.models.response.TokenResponse
import io.correcta.identities.security.annotations.IsLoggedUser
import io.correcta.identities.security.components.providers.user.models.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication operations endpoint.")
interface AuthenticationController {

    @Operation(
        summary = "Realiza o login do usuário",
        description = "Autentica o usuário e retorna um token de acesso."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Login realizado com sucesso",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = TokenResponse::class))]
            ),
            ApiResponse(responseCode = "401", description = "Credenciais inválidas")]
    )
    @PostMapping("/login")
    fun loginUser(
        request: HttpServletRequest,
        @Valid @RequestBody body: LoginUserRequest
    ): ResponseEntity<TokenResponse>

    @Operation(
        summary = "Realiza o logout do usuário",
        description = "Encerra a sessão do usuário, invalidando o token de acesso e o token de atualização."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "Logout realizado com sucesso"
            ),
            ApiResponse(responseCode = "401", description = "Token de acesso inválido ou expirado")]
    )
    @PostMapping("/logout")
    @IsLoggedUser
    fun logoutUser(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<Void>

    @Operation(
        summary = "Atualiza o token de acesso",
        description = "Recebe um token de atualização válido e retorna um novo token de acesso."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Token de acesso atualizado com sucesso",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = TokenResponse::class))]
            ),
            ApiResponse(responseCode = "401", description = "Token de atualização inválido ou expirado")]
    )
    @PostMapping("/refresh-token")
    fun refreshAccessToken(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<TokenResponse>

}