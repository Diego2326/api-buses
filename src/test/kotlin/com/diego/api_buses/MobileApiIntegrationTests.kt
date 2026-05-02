package com.diego.api_buses

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
class MobileApiIntegrationTests {
    @Autowired
    lateinit var mockMvc: MockMvc

    private val objectMapper = ObjectMapper()

    @Test
    fun `registers passenger and resolves bus by code`() {
        val token = registerPassenger("ana.mobile@buses.gt").first

        mockMvc.perform(
            get("/api/v1/buses/by-code/BUS-102")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("BUS-102"))
            .andExpect(jsonPath("$.route.name").value("Ruta 12 Centro"))
            .andExpect(jsonPath("$.route.origin").value("Terminal Oriente"))
            .andExpect(jsonPath("$.route.destination").value("Parque Central"))
    }

    @Test
    fun `lists stops without search filter`() {
        val token = registerPassenger("stops.mobile@buses.gt").first

        mockMvc.perform(
            get("/api/v1/stops")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].code").exists())
            .andExpect(jsonPath("$.totalElements").value(8))
    }

    @Test
    fun `soft deletes stop through admin endpoint`() {
        val token = loginAdmin()

        mockMvc.perform(
            delete("/api/v1/stops/10000000-0000-0000-0000-000000000001")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("10000000-0000-0000-0000-000000000001"))
            .andExpect(jsonPath("$.status").value("INACTIVE"))
    }

    @Test
    fun `updates and deletes non wallet payment administratively`() {
        val token = loginAdmin()
        val updatedDate = Instant.parse("2026-04-29T12:00:00Z")

        mockMvc.perform(
            put("/api/v1/payments/60000000-0000-0000-0000-000000000001")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "userId": "00000000-0000-0000-0000-000000000002",
                      "busId": "40000000-0000-0000-0000-000000000118",
                      "amount": 5.50,
                      "method": "CASH",
                      "status": "FAILED",
                      "date": "$updatedDate",
                      "externalReference": "admin-fix-001"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("60000000-0000-0000-0000-000000000001"))
            .andExpect(jsonPath("$.bus").value("BUS-118"))
            .andExpect(jsonPath("$.amount").value(5.5))
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.method").value("CASH"))

        mockMvc.perform(
            delete("/api/v1/payments/60000000-0000-0000-0000-000000000001")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        mockMvc.perform(
            get("/api/v1/payments/60000000-0000-0000-0000-000000000001")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `supports passenger wallet top up and wallet payment flow`() {
        val (token, userId) = registerPassenger("wallet.mobile@buses.gt")

        mockMvc.perform(
            post("/api/v1/wallet/top-ups")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "amount": 20.0,
                      "method": "CARD"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.amount").value(20.0))
            .andExpect(jsonPath("$.status").value("COMPLETED"))

        mockMvc.perform(
            get("/api/v1/wallet")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.balance").value(20.0))
            .andExpect(jsonPath("$.currency").value("GTQ"))

        mockMvc.perform(
            post("/api/v1/payments")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "userId": "$userId",
                      "busId": "40000000-0000-0000-0000-000000000102",
                      "amount": 4.0,
                      "method": "WALLET",
                      "externalReference": "mobile-wallet-001"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(userId))
            .andExpect(jsonPath("$.busId").value("40000000-0000-0000-0000-000000000102"))
            .andExpect(jsonPath("$.bus").value("BUS-102"))
            .andExpect(jsonPath("$.busPlate").value("C 102 BAA"))
            .andExpect(jsonPath("$.routeName").value("Ruta 12 Centro"))
            .andExpect(jsonPath("$.routeOrigin").value("Terminal Oriente"))
            .andExpect(jsonPath("$.routeDestination").value("Parque Central"))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.method").value("WALLET"))

        val walletAfterPayment = mockMvc.perform(
            get("/api/v1/wallet")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andReturn()

        val walletBalance = json(walletAfterPayment.response.contentAsString)["balance"].decimalValue()
        assertEquals(BigDecimal("16.00"), walletBalance.setScale(2))

        mockMvc.perform(
            get("/api/v1/wallet/transactions")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].type").value("PAYMENT"))
            .andExpect(jsonPath("$.content[0].amount").value(4.0))
            .andExpect(jsonPath("$.content[1].type").value("TOP_UP"))
            .andExpect(jsonPath("$.content[1].amount").value(20.0))
    }

    private fun registerPassenger(email: String): Pair<String, String> {
        val result = mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Ana Mobile",
                      "email": "$email",
                      "password": "123456"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.role").value("PASSENGER"))
            .andExpect(jsonPath("$.user.status").value("ACTIVE"))
            .andReturn()

        val body = json(result.response.contentAsString)
        return body["token"].asText() to body["user"]["id"].asText()
    }

    private fun loginAdmin(): String {
        val result = mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "admin@buses.gt",
                      "password": "admin123"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andReturn()

        return json(result.response.contentAsString)["token"].asText()
    }

    private fun json(content: String): JsonNode = objectMapper.readTree(content)
}
