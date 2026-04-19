package dev.stat.chat;

import dev.stat.chat.dto.NutritionLogRequest;
import dev.stat.chat.service.ChatService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the NutritionLogResource endpoint.
 */
@QuarkusTest
class NutritionLogResourceTest {

    @InjectMock
    ChatService chatService;

    @Test
    void testLogNutrition_Success() {
        // Given
        String expectedMessage = "Mahlzeit erfasst! 450 kcal (35g Protein, 40g Carbs, 15g Fett)";
        when(chatService.processSilentNutritionLog(any(NutritionLogRequest.class)))
                .thenReturn(expectedMessage);

        // When/Then
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "description": "200g Hähnchenbrust mit Reis und Gemüse"
                        }
                        """)
                .when()
                .post("/api/nutrition/log")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("message", equalTo(expectedMessage));

        verify(chatService).processSilentNutritionLog(any(NutritionLogRequest.class));
    }

    @Test
    void testLogNutrition_EmptyDescription_Returns400() {
        // When/Then
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "description": ""
                        }
                        """)
                .when()
                .post("/api/nutrition/log")
                .then()
                .statusCode(400);
    }

    @Test
    void testLogNutrition_MissingDescription_Returns400() {
        // When/Then
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/nutrition/log")
                .then()
                .statusCode(400);
    }

    @Test
    void testLogNutrition_BlankDescription_Returns400() {
        // When/Then
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "description": "   "
                        }
                        """)
                .when()
                .post("/api/nutrition/log")
                .then()
                .statusCode(400);
    }

    @Test
    void testLogNutrition_ComplexMeal() {
        // Given
        String expectedMessage = "Mahlzeit erfasst! 850 kcal (60g Protein, 90g Carbs, 25g Fett)";
        when(chatService.processSilentNutritionLog(any(NutritionLogRequest.class)))
                .thenReturn(expectedMessage);

        // When/Then
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "description": "2 Scheiben Vollkornbrot mit Erdnussbutter, 1 Banane und 300ml Milch"
                        }
                        """)
                .when()
                .post("/api/nutrition/log")
                .then()
                .statusCode(200)
                .body("message", equalTo(expectedMessage));
    }
}
