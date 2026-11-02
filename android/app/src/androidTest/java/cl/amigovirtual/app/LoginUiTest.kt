package cl.amigovirtual.app
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
class LoginUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun invalidLoginShowsValidation() {
        compose.waitUntil(15000) { compose.onAllNodesWithText("Iniciar sesión").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Iniciar sesión").performClick()
        compose.onNodeWithText("Introduce un correo válido y una contraseña.").assertExists()
    }
    @Test fun registerNeverOffersAdminRole() {
        compose.waitUntil(15000) { compose.onAllNodesWithText("Solicitar acceso · Crear cuenta").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Solicitar acceso · Crear cuenta").performScrollTo().performClick()
        compose.onNodeWithText("La cuenta se crea sin permisos. Un administrador autorizará tu acceso.").assertExists()
        compose.onNodeWithText("administrador").assertDoesNotExist()
    }
}
