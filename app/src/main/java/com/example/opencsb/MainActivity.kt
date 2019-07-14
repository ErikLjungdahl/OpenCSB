package com.example.opencsb

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
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import java.net.CookieHandler
import java.net.CookieManager

import com.google.android.gms.tasks.Task
import androidx.annotation.NonNull
import com.google.android.gms.auth.api.credentials.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException


class MainActivity : AppCompatActivity() {

    lateinit var mCredentialsClient: CredentialsClient
    lateinit var mCredentialRequest: CredentialRequest

    private val TAG = "MainActivity"
    private val RC_SAVE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        CookieHandler.setDefault(CookieManager())

        mCredentialsClient = Credentials.getClient(this)
        mCredentialRequest = CredentialRequest.Builder()
            .setPasswordLoginSupported(true)
            .build()

        fun onCredentialRetrieved(credential: Credential) {
            val accountType = credential.accountType
            if (accountType == null) {
                // Sign the user in with information from the Credential.
                val pwd = credential.password
                if (pwd != null) {
                    login_csb(credential.id, pwd)
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




    fun new_login(view: View) {
        val personnummer = findViewById<EditText>(R.id.input_personnummer).text.toString()
        val pwd = findViewById<EditText>(R.id.input_pwd).text.toString()
        val store = findViewById<CheckBox>(R.id.input_store).isChecked

        if (store) {
            val credential = Credential.Builder(personnummer)
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
                            Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Request has no resolution
                        Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }


        login_csb(personnummer, pwd)
    }

    fun login_csb(personnummer : String, pwd : String) {

        // TextView for errors
        val textView = findViewById<TextView>(R.id.output_result)
        // textView.movementMethod = ScrollingMovementMethod()

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
                    val id = "1720" + (0..9999999999999999).random().toString()
                    val unixTime = System.currentTimeMillis().toString()

                    val completeUrl = "https://www.chalmersstudentbostader.se/widgets/?callback=jQuery" + id + "_" + unixTime + "&widgets%5B%5D=aptuslogin%40APTUSPORT"
/*                  // Doesn't seem to work, not exactly a json that gets returned
                    val jquery = JsonObjectRequest(Request.Method.GET, completeUrl, null,
                    Response.Listener<JSONObject> { response2 ->

                            textView.text = "Response: %s".format(response2.toString())
                        },
                        Response.ErrorListener { textView.text = "JQuery didn't work!"
                        }
                    )
                    queue.add(jquery)
*/
                    val jquery = StringRequest(Request.Method.GET, completeUrl,
                        Response.Listener<String> { response2 ->
                             val json = (response2.dropWhile { c -> c != '(' }
                                .drop(1)
                                .takeWhile { c -> c != ')' })
                             val i = json.indexOf("aptusUrl") + 11
                             val aptusUrl = json.drop(i).takeWhile { c -> c != '"' }

                             startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(aptusUrl)))
                        },
                        Response.ErrorListener { textView.text = "JQuery didn't work!"
                        }
                    )
                    queue.add(jquery)

                } else {
                    textView.text = "Login failed"
                }
            },
            Response.ErrorListener { textView.text = "That didn't work!"
            },
            params
            )
        // Add the request to the RequestQueue.
        queue.add(stringRequest)

        /*
        val intent = Intent(this, Ports::class.java).apply {
            putExtra(EXTRA_MESSAGE, message)
        }
        startActivity(intent)
        */
    }
}
