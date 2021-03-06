package erikljungdahl.opencsb

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.com.simplepass.loadingbutton.customViews.CircularProgressButton
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import java.net.CookieHandler
import java.net.CookieManager

import com.google.android.gms.auth.api.credentials.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.common.api.ResolvableApiException


class MainActivity : AppCompatActivity() {

    lateinit var mCredentialsClient: CredentialsClient
    lateinit var mCredentialRequest: CredentialRequest
    lateinit var btn : CircularProgressButton

    private val TAG = "MainActivity"
    private val RC_SAVE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        CookieHandler.setDefault(CookieManager())

        val options = CredentialsOptions.Builder()
            .forceEnableSaveDialog()
            .build()
        mCredentialsClient = Credentials.getClient(this, options)
        mCredentialRequest = CredentialRequest.Builder()
            .setPasswordLoginSupported(true)
            .build()

        fun onCredentialRetrieved(credential: Credential) {
            val accountType = credential.accountType
            if (accountType == null) {
                // Sign the user in with information from the Credential.
                if (credential.password != null) {
                    findViewById<EditText>(R.id.input_personnummer).setText(credential.id)
                    findViewById<EditText>(R.id.input_pwd).setText(credential.password)
                    loginCsb(credential.id, credential.password!!)
                }
            }
        }

        mCredentialsClient.request(mCredentialRequest).addOnCompleteListener(
            OnCompleteListener { task ->
                if (task.isSuccessful) {
                    // See "Handle successful credential requests"
                    onCredentialRetrieved(task.result!!.credential)
                    return@OnCompleteListener
                } else {
                    // Needs to login
                }
            })
    }




    fun newLogin(view: View) {
        // personal identity number (Swedish: personnummer)
        val personalNum = findViewById<EditText>(R.id.input_personnummer).text.toString()
        val pwd = findViewById<EditText>(R.id.input_pwd).text.toString()
        val store = findViewById<CheckBox>(R.id.input_store).isChecked

        if (store) {
            val credential = Credential.Builder(personalNum)
                .setPassword(pwd)  // Important: only store passwords in this field.
                // Android autofill uses this value to complete
                // sign-in forms, so repurposing this field will
                // likely cause errors.
                .build()

            mCredentialsClient.save(credential).addOnCompleteListener(
                OnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "SAVE: OK")
                        Toast.makeText(this, "Credentials saved", Toast.LENGTH_SHORT).show()
                        return@OnCompleteListener
                    }
                    val e = task.exception
                    if (e is ResolvableApiException) {
                        // Try to resolve the save request. This will prompt the user if
                        // the credential is new
                        try {
                            e.startResolutionForResult(this, RC_SAVE)
                        } catch (sie: IntentSender.SendIntentException) {
                            // Could not resolve the request
                            Log.e(TAG, "Failed to send resolution.", sie)
                            Toast.makeText(this, "Save failed: Couldn't resolve", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Request has no resolution
                        Toast.makeText(this, "Save failed: No resolution", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }


        loginCsb(personalNum, pwd)
    }

    @SuppressLint("SetTextI18n")
    fun loginCsb(personnummer : String, pwd : String) {
        val doorID = intent.getStringExtra("doorID")
        // TextView for errors
        val textView = findViewById<TextView>(R.id.output_result)
        // textView.movementMethod = ScrollingMovementMethod()

        btn = findViewById(R.id.button_login)
        btn.startAnimation();

        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(this)
        val url = "https://www.chalmersstudentbostader.se/wp-login.php"

        // Not ideal data structure since it makes the order in hash-order.
        // Should use an array of pairs, so the order remains as intended, but how does that work in Kotlin together with Java
        val params = hashMapOf<String, String>()
        params["log"] = personnummer
        params["pwd"] = pwd
        // params["redirect_to"] = "https://www.chalmersstudentbostader.se/min-bostad/" // makes it not work

        // Request a string response from the provided URL.
        val stringRequest = StringRequestParam(Request.Method.POST, url,
            Response.Listener<String> { response ->
                // See if login succeeded and we got redirected to "Min bostad"
                if(response.contains("<title>\n" + "Chalmers")) {
                    // id and unixtime are generated to mimic CSB's behavior, don't know if it is actually needed
                    val id = "1720" + (0..9999999999999999).random().toString()
                    val unixTime = System.currentTimeMillis().toString()

                    val completeUrl = "https://www.chalmersstudentbostader.se/widgets/?callback=jQuery" +
                                              id + "_" + unixTime + "&widgets%5B%5D=aptuslogin%40APTUSPORT"

                    // Requesting the widget that supplies the aptuslogin url
                    val jquery = StringRequest(Request.Method.GET, completeUrl,
                        Response.Listener<String> { response2 ->
                            val json = (response2.dropWhile { c -> c != '(' }
                                .drop(1)
                                .takeWhile { c -> c != ')' })
                            val i = json.indexOf("aptusUrl") + 11
                            val aptusUrl = json.drop(i).takeWhile { c -> c != '"' }

                            // Automatic or manual unlock?
                            if (doorID != null) {
                                // Open Aptus for cookies
                                val openAptus = StringRequest(Request.Method.GET, aptusUrl,
                                    Response.Listener { _ ->
                                        //Should probably check if I get OK

                                        val aptUrl =
                                            "https://apt-www.chalmersstudentbostader.se/AptusPortal/Lock/UnlockEntryDoor/$doorID"
                                        // Automatically unlock a specific door
                                        val openDoorRequest = StringRequest(Request.Method.GET, aptUrl,
                                            Response.Listener { _ ->
                                                // Maybe should check the response
                                                textView.text = "Door Unlocked"
                                                btn.revertAnimation()
                                                // Below stopped working for unknown reason
                                                /* textView.text = response4.dropWhile { c -> c != ':' }.
                                                                          drop(2).
                                                                          takeWhile { c -> c != '"' }
                                                */
                                            },
                                            Response.ErrorListener {
                                                textView.text = "Opening Door automatically didn't work, redirecting to manual unlock"
                                                btn.revertAnimation()
                                                manualUnlock(aptusUrl)
                                            })
                                        queue.add(openDoorRequest)
                                    },
                                    Response.ErrorListener {
                                        textView.text = "Opening Aptus didn't work"
                                        btn.revertAnimation()
                                    }
                                )
                                queue.add(openAptus)
                            } else {
                                // Manually unlock
                                manualUnlock(aptusUrl)
                                btn.revertAnimation()
                            }
                        },
                        Response.ErrorListener {
                            textView.text = "Login succeeded but URL to aptus could not be fetched"
                            btn.revertAnimation()
                        }
                    )
                    queue.add(jquery)

                } else {
                    textView.text = "Login failed"
                    btn.revertAnimation()
                }
            },
            Response.ErrorListener {
                textView.text = "Could not access CSB's Website"
                btn.revertAnimation()
            },
            params
            )
        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }

    private fun manualUnlock (url : String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    fun openSettings (view: View) {
        val intent = Intent(applicationContext, SettingsActivity::class.java)
        startActivity(intent)
    }



}

