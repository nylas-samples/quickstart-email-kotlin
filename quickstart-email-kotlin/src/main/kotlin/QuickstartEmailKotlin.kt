import com.google.gson.Gson
import com.nylas.NylasClient
import com.nylas.models.*
import io.github.cdimascio.dotenv.dotenv
import spark.kotlin.Http
import spark.kotlin.ignite

fun main(args: Array<String>) {
    val dotenv = dotenv()

    val nylas: NylasClient = NylasClient(
        apiKey = dotenv["NYLAS_API_KEY"],
        apiUri = dotenv["NYLAS_API_URI"]
    )

    val http: Http = ignite()

    http.get("/nylas/auth") {
        val scope = listOf("https://www.googleapis.com/auth/gmail.modify")
        val config = UrlForAuthenticationConfig(
            dotenv["NYLAS_CLIENT_ID"],
            "http://localhost:4567/oauth/exchange",
            AccessType.ONLINE,
            AuthProvider.GOOGLE,
            Prompt.DETECT,
            scope,
            true,
            "sQ6vFQN",
            "swag@nylas.com")

        val url = nylas.auth().urlForOAuth2(config)
        response.redirect(url)
    }

    http.get("/oauth/exchange") {
        val code : String = request.queryParams("code")
        if(code == "") { response.status(401) }
        val codeRequest : CodeExchangeRequest = CodeExchangeRequest(
            "http://localhost:4567/oauth/exchange",
            code,
            dotenv["NYLAS_CLIENT_ID"],
            null,
            null
        )
        try {
            val codeResponse : CodeExchangeResponse = nylas.auth().exchangeCodeForToken(codeRequest)
            request.session().attribute("grant_id",codeResponse.grantId)
            codeResponse.grantId
        }catch (e : Exception){
            e.toString()
        }
    }

    http.get("/nylas/recent-emails") {
        try {
            val queryParams = ListMessagesQueryParams.Builder().limit(5).build()
            val emails = nylas.messages().list(request.session().attribute("grant_id"), queryParams)
            val gson = Gson()
            gson.toJson(emails.data)
        }catch (e : Exception){
            e.toString()
        }
    }

    http.get("/nylas/send-email") {
        try {
            val emailNames : List<EmailName> = listOf(EmailName(dotenv["EMAIL"], "Name"))
            val requestBody : SendMessageRequest = SendMessageRequest.Builder(emailNames).
            replyTo(listOf(EmailName(dotenv["EMAIL"], "Name"))).
            subject("Your Subject Here").
            body("Your email body here").
            build()
            val email = nylas.messages().send(request.session().attribute("grant_id"),requestBody)
            val gson = Gson()
            gson.toJson(email.data)
        }catch (e : Exception){
            e.toString()
        }
    }
}