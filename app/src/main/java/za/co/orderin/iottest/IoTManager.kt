package digital.nona.iottest
import android.content.Context
import android.util.Log
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.*
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager
import com.amazonaws.regions.Regions
import com.auth0.android.jwt.JWT
import java.lang.Exception
import kotlin.concurrent.thread

class IoTManager(private val context: Context) {

    private val cognitoUserPool = CognitoUserPool(context,COGNITO_USER_POOL_ID,COGNITO_APP_CLIENT_ID,COGNITO_APP_CLIENT_SECRET,COGNITO_AWS_REGION)

    val credentialsProvider: CognitoCachingCredentialsProvider = CognitoCachingCredentialsProvider(context, COGNITO_IDENTITY_POOL_ID, COGNITO_AWS_REGION)

    var awsIotMqttManager: AWSIotMqttManager? = null

    var subscribed = false

    var connected = false

    init {
        Log.i(TAG,"Initialising")
        login()
    }

    private fun login() {
        thread(start = true) {
            val user = cognitoUserPool.getUser(USERNAME)
            user.getSession(object : AuthenticationHandler {
                override fun onSuccess(userSession: CognitoUserSession?, newDevice: CognitoDevice?) {
                    Log.i(TAG, "Login success")
                    credentialsProvider.logins = hashMapOf<String, String>(Pair(COGNITO_USER_POOL_URL, userSession!!.idToken.jwtToken))
                    if (!connected) {
                        connected = true
                        connectIOT(userSession!!)
                    }
                }

                override fun onFailure(exception: Exception?) {
                    Log.i(TAG, "Login failure: $exception")
                }

                override fun getAuthenticationDetails(
                    authenticationContinuation: AuthenticationContinuation?,
                    UserId: String?
                ) {
                    Log.i(TAG, "Login continuation")
                    authenticationContinuation!!.setAuthenticationDetails(
                        AuthenticationDetails(
                            USERNAME,
                            PASSWORD,
                            hashMapOf()
                        )
                    )
                    authenticationContinuation.continueTask()
                }

                override fun authenticationChallenge(continuation: ChallengeContinuation?) {
                    Log.i(TAG, "Login authentication challenge")
                }

                override fun getMFACode(continuation: MultiFactorAuthenticationContinuation?) {
                    Log.i(TAG, "Login MFA code")
                }
            })
        }
    }

    private fun connectIOT(session: CognitoUserSession) {
        val jwt = JWT(session.idToken.jwtToken)
        val sub = jwt.getClaim("sub").asString()!!
        Log.i(TAG, "Got sub - $sub, connecting to IOT")
        awsIotMqttManager = AWSIotMqttManager(sub, IOT_ENDPOINT)
        awsIotMqttManager!!.keepAlive = 10
        awsIotMqttManager!!.setReconnectRetryLimits(60,60)
        awsIotMqttManager!!.isAutoReconnect = true
        awsIotMqttManager!!.maxAutoReconnectAttempts = -1

        awsIotMqttManager!!.connect(credentialsProvider, object : AWSIotMqttClientStatusCallback {
            override fun onStatusChanged(status: AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus?, throwable: Throwable?) {
                Log.i(TAG, "IoT Status: $status")
                // To trigger new user pool credentials if required when reconnecting
                // login()
            }
        })
    }

    companion object {
        private const val TAG = "IOTManager"

        private val COGNITO_USER_POOL_ID = ""
        private val COGNITO_APP_CLIENT_ID = ""
        private val COGNITO_APP_CLIENT_SECRET = ""
        private val COGNITO_USER_POOL_URL = ""
        private val COGNITO_IDENTITY_POOL_ID = ""
        private val COGNITO_AWS_REGION = Regions.EU_WEST_1
        private val IOT_ENDPOINT = ""

        private val USERNAME=""
        private val PASSWORD=""
    }
}