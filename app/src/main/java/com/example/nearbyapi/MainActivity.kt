package com.example.nearbyapi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.nearbyapi.adapter.EndpointListAdapter
import com.example.nearbyapi.model.Endpoint
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy

class MainActivity : AppCompatActivity() {

    private lateinit var connectionsClient: ConnectionsClient
    private lateinit var endpointListAdapter: EndpointListAdapter

    private var isAdvertising: Boolean = false
    private var isDiscovering: Boolean = false
    private var endPointId: String? = null
    private var EMIT_NAME: String  = "Faiz"
    private var SERVICE_ID: String?= "com.dooropen.mobile"

    private lateinit var localEndpointId: TextView
    private lateinit var availableDevicesText: TextView
    private lateinit var status: TextView
    private lateinit var inputMessage: EditText
    private lateinit var sendMessage: Button
    private lateinit var endpointsListView: ListView

    private val PERMISSIONS_REQUEST_CODE = 1234

    private val TAG = MainActivity:: class.java.simpleName+"logs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.connection_status_text)
        inputMessage = findViewById(R.id.et_message)
        availableDevicesText = findViewById(R.id.available_devices)
        localEndpointId = findViewById(R.id.localEndpointId)
        endpointsListView = findViewById(R.id.enpointsList)

        //Request for the necessary permissions
        checkPermissions()
        requestPermissions()

        //Initialize the nearby connections api client
        connectionsClient = Nearby.getConnectionsClient(this)

        //Add a click listener to the "Advertise" button
        val advertiseButton = findViewById<Button>(R.id.advertise_button)
        advertiseButton.setOnClickListener{
            if(!isAdvertising && !isDiscovering){
                startAdvertising()
                advertiseButton.text = "Stop Advertising"
            } else {
                stopAdvertising()
                advertiseButton.text = "Start Advertising"
            }
        }

        //Add a click listener to the "Discover" button
        val discoverButton = findViewById<Button>(R.id.discover_button)
        discoverButton.setOnClickListener{
            if(!isAdvertising && !isDiscovering){
                startDiscovering()
                discoverButton.text = "Stop Discovering"
            } else {
                stopDiscovering()
                discoverButton.text = "Start Discovering"
            }
        }

        //Add list to the EndpointListAdapter
        endpointListAdapter = EndpointListAdapter(this, mutableListOf())

        endpointsListView.adapter = endpointListAdapter

        endpointsListView.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                val endpoint = endpointListAdapter.getItem(position)
                Log.d(TAG, "onCreate: ${endpoint.name}")
                connectionsClient.requestConnection(EMIT_NAME, endpoint.name, connectionLifecycleCallback)
            }

        //Add message to the input and send the payload to connected device
        sendMessage = findViewById<Button>(R.id.send_button)
        sendMessage.setOnClickListener{
            sendPayload()
            showToast("Send payload to $endPointId: ${inputMessage.text}")
        }
    }

    private fun checkPermissions(): Boolean {
        Log.e(TAG, "checkPermissions: ")
        val bluetoothPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
        val bluetoothAdminPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
        val coarseLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        val fineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val wifiStatePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE)
        val changeWifiStatePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE)

        return (bluetoothPermission == PackageManager.PERMISSION_GRANTED &&
                bluetoothAdminPermission == PackageManager.PERMISSION_GRANTED &&
                coarseLocationPermission == PackageManager.PERMISSION_GRANTED &&
                fineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                wifiStatePermission == PackageManager.PERMISSION_GRANTED &&
                changeWifiStatePermission == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPermissions() {
        Log.e(TAG, "requestPermissions: ")
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE
            ),
            PERMISSIONS_REQUEST_CODE
        )
    }

    private fun stopDiscovering() {
        connectionsClient.stopDiscovery()
        isDiscovering = false
        Log.d(TAG, "stopDiscovering: $isDiscovering")

        availableDevicesText.visibility = View.GONE
        endpointsListView.visibility = View.GONE
    }

    private fun startDiscovering() {
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()

        connectionsClient.startDiscovery(SERVICE_ID!!, endpointDiscoveryCallback, discoveryOptions)
            .addOnSuccessListener {
                isDiscovering = true
                Log.d(TAG, "startDiscovering: $isDiscovering")

                availableDevicesText.visibility = View.VISIBLE
                endpointsListView.visibility = View.VISIBLE
            }
            .addOnFailureListener {
                isDiscovering = false
                Log.d(TAG, "startDiscovering: $isDiscovering")
            }
    }

    private fun stopAdvertising() {
        connectionsClient.stopAdvertising()
        isAdvertising = false
        Log.d(TAG, "stopAdvertising: $isAdvertising")

        availableDevicesText.visibility = View.GONE
        endpointsListView.visibility = View.GONE
    }

    private fun startAdvertising() {
        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()

        connectionsClient.startAdvertising(EMIT_NAME, SERVICE_ID!!, connectionLifecycleCallback, advertisingOptions)

            .addOnSuccessListener {
                isAdvertising = true
                Log.d(TAG, "startAdvertising: $isAdvertising")



            }.addOnFailureListener {
                isAdvertising = false
                Log.d(TAG, "startAdvertising: $isAdvertising")
            }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback(){
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {

            Log.d(TAG,endpointId+" Connection Info Name "+connectionInfo.getEndpointName()+
                    " Connection getAuthenticationDigits "+connectionInfo.getAuthenticationDigits()+
                    " Connection getAuthenticationToken "+connectionInfo.getAuthenticationToken());

            SERVICE_ID = connectionInfo.endpointName
            status.text = "Connected to $SERVICE_ID"
            // Automatically accept the connection on both sides
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            endPointId = endpointId
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            Log.d(TAG, "onConnectionResult: "+endpointId+" Status "+result.getStatus())
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {

                    inputMessage.visibility = View.VISIBLE
                    sendMessage.visibility = View.VISIBLE

                    connectionsClient.stopDiscovery()
                    isDiscovering = false
                    connectionsClient.stopAdvertising()
                    isAdvertising = false

                    availableDevicesText.visibility = View.GONE
                    endpointsListView.visibility = View.GONE
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    status.text = "Connection rejected by $SERVICE_ID"
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    status.text = "Error connecting to $SERVICE_ID"
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "onDisconnected: "+endpointId)
            status.text = "Disconnected from $SERVICE_ID"
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback(){
        override fun onEndpointFound(p0: String, p1: DiscoveredEndpointInfo) {
            Log.d(TAG, "onEndpointFound: Local Endpoint "+p0+",Discovered onces "+p1.endpointName+""+p1.serviceId)

            endPointId?.let { connectionsClient.requestConnection(EMIT_NAME, it, connectionLifecycleCallback)
            }

            val endpoint = Endpoint(p1.endpointName, p0)
            endpointListAdapter.updateEndpoints(listOf(endpoint))
        }

        override fun onEndpointLost(p0: String) {
            endpointListAdapter.updateEndpoints(emptyList())
        }

    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val message = String(payload.asBytes()!!)
            Toast.makeText(applicationContext, "Message received: $message", Toast.LENGTH_LONG).show()
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Update UI here if necessary
        }
    }

    private fun sendPayload(){
        val payload = Payload.fromBytes(inputMessage.text.toString().toByteArray())
        endPointId?.let { connectionsClient.sendPayload(it, payload) }
    }

    private fun showToast(s: String) {
        Toast.makeText(this@MainActivity, s, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()

        stopAdvertising()
        stopDiscovering()
    }
}