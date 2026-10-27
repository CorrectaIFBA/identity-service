package io.correcta.identities

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ProfileLookupTests(
    @Autowired val mockMvc: MockMvc
) {

    @Test
    fun `rejects lookups without a valid internal token`() {
        mockMvc.get("/api/v1/profiles/unknown").andExpect { status { isUnauthorized() } }

        mockMvc.get("/api/v1/profiles/unknown") {
            header("X-Internal-Token", "wrong-token")
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `reports unknown profiles`() {
        mockMvc.get("/api/v1/profiles/unknown") {
            header("X-Internal-Token", "test-internal-token")
        }.andExpect { status { isNotFound() } }

        mockMvc.post("/api/v1/profiles/batch-lookup") {
            header("X-Internal-Token", "test-internal-token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"profileIds": ["unknown-a", "unknown-b", "unknown-a"]}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.profiles.length()") { value(0) }
            jsonPath("$.notFoundProfileIds.length()") { value(2) }
        }
    }

    @Test
    fun `searches profiles by a term of at least two characters`() {
        mockMvc.get("/api/v1/profiles/search?q=a") {
            header("X-Internal-Token", "test-internal-token")
        }.andExpect { status { isBadRequest() } }

        mockMvc.get("/api/v1/profiles/search?q=nobody-matches-this&type=STUDENT") {
            header("X-Internal-Token", "test-internal-token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(0) }
        }

        mockMvc.get("/api/v1/profiles/search?q=ana").andExpect { status { isUnauthorized() } }
    }
}
