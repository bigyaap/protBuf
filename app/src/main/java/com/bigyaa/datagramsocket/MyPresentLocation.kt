package com.bigyaa.datagramsocket

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.activity_mypresentlocation.*
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class MyPresentLocation : AppCompatActivity() {
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var lastLocation: Location? = null
    private var latitudeLabel: String? = null
    private var longitudeLabel: String? = null
    private var latitudeText: TextView? = null
    private var longitudeText: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mypresentlocation)
        latitudeLabel = resources.getString(R.string.latitudeBabel)
        longitudeLabel = resources.getString(R.string.longitudeBabel)
        latitudeText = findViewById<View>(R.id.txtlatitudeText) as TextView
        longitudeText = findViewById<View>(R.id.txtlongitudeText) as TextView
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    public override fun onStart() {
        super.onStart()
        if (!checkPermissions()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions()
            }
        } else {
            getLastLocation()
        }
    }

    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_ACCESS_FINE_LOCATION
            )
            return
        }
        fusedLocationClient?.lastLocation!!.addOnCompleteListener(this) { task ->
            if (task.isSuccessful && task.result != null) {
                lastLocation = task.result
                latitudeText!!.text = latitudeLabel + ": " + (lastLocation)!!.latitude
                longitudeText!!.text = longitudeLabel + ": " + (lastLocation)!!.longitude
                val first = ((lastLocation)!!.latitude).toBigDecimal().toPlainString()
                val second = ((lastLocation)!!.longitude).toBigDecimal().toPlainString()
                val list: MutableList<String> = ArrayList()
                list.add(first)
                list.add(second)
                val task = AsynTaskForSocket(this)
                task.execute(list)
            } else {
                Log.w(TAG, "getLastLocation:exception", task.exception)
                showMessage("No location detected. Make sure location is enabled on the device.")
            }
        }
    }

    private fun showMessage(string: String) {
        val container = findViewById<View>(R.id.linearLayout)
        if (container != null) {
            Toast.makeText(this, string, Toast.LENGTH_LONG).show()
        }
    }

    private fun showSnackbar(
        mainTextStringId: String, actionStringId: String,
        listener: View.OnClickListener
    ) {
        Toast.makeText(this, mainTextStringId, Toast.LENGTH_LONG).show()
    }

    private fun checkPermissions(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
            REQUEST_PERMISSIONS_REQUEST_CODE
        )
    }

    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.")
            showSnackbar("Location permission is needed for core functionality", "Okay",
                View.OnClickListener {
                    startLocationPermissionRequest()
                })
        } else {
            Log.i(TAG, "Requesting permission")
            startLocationPermissionRequest()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            when {
                grantResults.isEmpty() -> {
                    // If user interaction was interrupted, the permission request is cancelled and you
                    // receive empty arrays.
                    Log.i(TAG, "User interaction was cancelled.")
                }
                grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                    // Permission granted.
                    getLastLocation()
                }
                else -> {
                    showSnackbar("Permission was denied", "Settings",
                        View.OnClickListener {
                            // Build intent that displays the App settings screen.
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts(
                                "package",
                                Build.DISPLAY, null
                            )
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }

    companion object {
        private val TAG = "LocationProvider"
        private val REQUEST_PERMISSIONS_REQUEST_CODE = 34
        private const val PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 100

        class AsynTaskForSocket internal constructor(context: MyPresentLocation) :
            AsyncTask<MutableList<String>, String, String?>() {

            var first: MutableList<String>? = null
            private var resp: String? = null
            private val activityReference: WeakReference<MyPresentLocation> = WeakReference(context)

            override fun onPreExecute() {
                val activity = activityReference.get()
                if (activity == null || activity.isFinishing) return
                activity.progressBar.visibility = View.VISIBLE
            }

            override fun doInBackground(vararg params: MutableList<String>?): String? {

                try {

                    for (item in params) { // input is an Array
                        first = params.get(0)
                    }
                    val latitudevalue: String? = first?.get(0)
                    val longitudevalue: String? = first?.get(1)

                    Thread.sleep(1000)
                    val person = AddressBookProtos.Person.newBuilder()
                        .setId(1)
                        .setLatitude(latitudevalue)
                        .setLongitude(longitudevalue)
                        .build()

                    val addressbook = AddressBookProtos.AddressBook.newBuilder()
                        .addAllPeople(listOf(person))
                        .build()

                    val bytes = addressbook.toByteArray()

                    val udpSocket = DatagramSocket(2222)
                    val serverAddr = InetAddress.getByName("68.183.91.197")
                    val packet = DatagramPacket(bytes, bytes.size, serverAddr, 2222)
                    udpSocket.send(packet)
                    resp = "Connected and sent"
                    println(resp)
                } catch (ex: IOException) {
                    println("Client error: " + ex.message)
                    ex.printStackTrace()
                }
                return resp
            }

            override fun onPostExecute(result: String?) {
                println("Coordinates sent")
                val activity = activityReference.get()
                if (activity == null || activity.isFinishing) return
                activity.progressBar.visibility = View.GONE
                activity.txtSent.visibility = View.VISIBLE
            }

            override fun onProgressUpdate(vararg text: String?) {

            }
        }
    }
}