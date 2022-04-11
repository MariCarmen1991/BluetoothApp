package com.example.bluetoothapp
import android.Manifest
import android.Manifest.permission.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.ACTION_FOUND
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler

import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothapp.adapter.DevicesAdapter
import com.example.bluetoothapp.data.Device
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {


    private val TAG = "BLUETOOTH_MC"
    //private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    val REQUEST_BLUETOOTH_PERMISSION: Int = 1
    private lateinit var bluetoothImage: ImageView
    private lateinit var bluetoothStatus: TextView
    private lateinit var listDevice: TextView
    private lateinit var permissionsState: TextView
    private lateinit var progressVar: ProgressBar
    private lateinit var titleDevice: TextView
    private lateinit var scanBle: Button
    private lateinit var connected: Button
    private lateinit var searchDevice: Button
    private lateinit var connectedDevices: Button
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var devicesAdapter: DevicesAdapter


    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }


    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Log.d("MariCarmen", "Request Permission result ok")
                registerBluetoothEvents()
                permissionsText("Se han concedido los permisos", true)

            } else {
                Log.d("MariCarmen", "Request Permission result result canceled")

            }
        }

    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("MariCarmen", "CONCEDIDO")
            } else {
                Log.d("MariCarmen", "nO CONCEDIDO")
            }
        }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            1 -> {

                Log.d("MariCarmen", "request:  " + requestCode)

                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) {

                    Log.d("MariCarmen", "granted")
                    permissionsText("Permissions Grant", true)

                } else {
                    Log.d("MariCarmen", "No granted")
                    permissionsText("Haven't Permissions", false)

                }
                return


            }
            else -> {
                permissionsText("Haven't Permissions", false)
            }


        }


    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //init buttons
        connected = findViewById(R.id.connect_id)
        connectedDevices = findViewById(R.id.connectDevice_id)
        bluetoothImage = findViewById(R.id.imageView)
        bluetoothStatus = findViewById(R.id.bluetoothStatus)
        permissionsState = findViewById(R.id.permissionsState)
        mRecyclerView = findViewById(R.id.recyclerview)
        searchDevice=findViewById(R.id.scan_id)
        progressVar=findViewById(R.id.progressBar)
        titleDevice=findViewById(R.id.title_1)
        scanBle=findViewById(R.id.buttonBle)
        titleDevice.visibility=View.GONE
        progressVar.visibility=View.GONE

        if (applicationContext.packageManager.hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH
            )
        ) {

            requestScanPermission()
            registerBluetoothEvents()
            comprobarBluetooth()
            encenderBluetooth()
            checkScanPermission()
            associateDevices()
            discoverDevices()
            discoverBleDevices()

        } else {
            // Gracefully degrade your app experience.
        }




    }



    fun comprobarBluetooth() {


        if (bluetoothAdapter?.isEnabled!!) {
            bluetoothStateText("Bluetooth is connected", true)
            Log.d("MariCarmen", "Bluetooth is  aviable")
        } else {
            bluetoothStateText("Bluetooth is not connected", false)
            Log.d("MariCarmen", "Bluetooth is not aviable")


        }


    }


    fun encenderBluetooth() {
        connected.setOnClickListener {

            if (bluetoothAdapter?.isEnabled!!) {
                Toast.makeText(this, "CONECTANDO BLUETOOTH", Toast.LENGTH_LONG).show()


            } else {

                var intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                activityResultLauncher.launch(intent)
                Toast.makeText(this, "DEBE CONECTAR BLUETOOTH", Toast.LENGTH_LONG).show()

            }
        }


    }

   private fun checkScanPermission() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("MariCarmen", "PEDIR PERMISOS BLUETOOTH")


        } else {
            Log.d("MariCarmen", "PERMISOS CONCECIDOS")

        }

        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("MariCarmen", "PEDIR PERMISOS FINE LOCATION")


        } else {

            permissionsText("Permissions Grant", true)

        }

    }


    private fun requestScanPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                BLUETOOTH_SCAN
            )
        ) {
            Toast.makeText(this, "Ve a ajustes para otorgar permisos", Toast.LENGTH_LONG).show()


        } else {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(BLUETOOTH_SCAN, ACCESS_FINE_LOCATION, BLUETOOTH_CONNECT),
                REQUEST_BLUETOOTH_PERMISSION
            )
            Log.d("MariCarmen", "LAUNCH")

            requestPermissionLauncher.launch(BLUETOOTH_SCAN)

        }


    }

    private val bleScanner = object: ScanCallback(){
        val bleDevices=ArrayList<Device>()

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val bleDevice= Device("","",0,"")
            titleDevice.visibility=View.VISIBLE
            titleDevice.text="BLE Devices"
            Log.d("MariCarmen","onScanResult: ${result?.device?.address} - ${result?.device?.name}")

            if(result?.device?.name==null){
                bleDevice.name="No Name"
                bleDevice.alias="No Name"
            }
            else{
                bleDevice.name= result.device.name
                bleDevice.alias=result.device.alias
            }
            bleDevice.adress= result?.device!!.address

            if(!bleDevices.contains(bleDevice)){

            bleDevices.add(bleDevice)}
            nRecyclerView(bleDevices)



        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)


        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
        }
    }





    private fun discoverBleDevices(){
        scanBle.setOnClickListener {

            val bluetoothLeScanner =bluetoothAdapter?.bluetoothLeScanner
            var mScanning = false
            val SCAN_PERIOD: Long = 10000

            if (bluetoothAdapter?.isEnabled!!) {

                Handler(Looper.getMainLooper()).postDelayed({
                    mScanning = false
                    bluetoothLeScanner?.stopScan(bleScanner)
                }, SCAN_PERIOD)

                mScanning = true
                bluetoothLeScanner?.startScan(bleScanner)
            }
            else{
                mScanning=false
                bluetoothLeScanner?.stopScan(bleScanner)
            }

        }


    }

    private fun response(){
        val mLeScanCallback= BluetoothAdapter.LeScanCallback { bluetoothDevice, i, bytes -> runOnUiThread{


            Log.d("MariCarmen", "dispositivos LE"+bluetoothDevice.name)
        } }

    }






    private fun discoverDevices(){
        searchDevice.setOnClickListener {
            titleDevice.visibility=View.VISIBLE
            titleDevice.text="discovering"


            if(bluetoothAdapter?.isDiscovering!!){
                bluetoothAdapter?.cancelDiscovery()
                this.unregisterReceiver(bluetoothDiscoverDevices)
                Toast.makeText(this, "Scan Again", Toast.LENGTH_SHORT).show()
                Log.d("MariCarmen", "Canceled")
            }
            else{
                Toast.makeText(this, "Iniciando búsqueda de dispositivos bluetooth", Toast.LENGTH_LONG).show()

                bluetoothAdapter?.startDiscovery()
                val filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED).apply {

                }
                filter.addAction(BluetoothDevice.ACTION_FOUND)
                filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)


                registerReceiver(bluetoothDiscoverDevices, filter)
            }

        }

    }


    private fun associateDevices() {

        connectedDevices.setOnClickListener {
            titleDevice.visibility=View.VISIBLE

            var lisOfDevices=ArrayList<Device>()

            var pairedDevices: Set<BluetoothDevice> = Collections.emptySet()
            try {
                pairedDevices = bluetoothAdapter?.bondedDevices!!

                pairedDevices.forEach {
                    var device=Device("","",0,"")
                    device.name= it.name
                    device.alias= it.alias!!
                    device.adress=it.address
                    device.type= it.type

                    lisOfDevices.add(device)
                    Log.d("MariCarmen", "${it.name}")
                }
            } catch (e: Exception) {
                Log.d(TAG, "EXCEPTION ${e}")
            }
            Log.d("MariCarmen", "list: ${lisOfDevices[0].name}")

            nRecyclerView(lisOfDevices)

        }


    }

    fun registerBluetoothEvents() {

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED).apply {

        }
        registerReceiver(bluetoothStateReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        this.unregisterReceiver(bluetoothStateReceiver)
    }

    private val bluetoothDiscoverDevices:BroadcastReceiver=object: BroadcastReceiver(){
        override fun onReceive(p0: Context?, p1: Intent?) {
            var devicesDiscovered = ArrayList<Device>()

            val action : String? = p1?.action
            Log.d("MariCarmen", "" +action+" Bluetooth device found"+BluetoothDevice.ACTION_FOUND)

            if(ACTION_FOUND.equals(action)){
                titleDevice.text = resources.getString(R.string.title_2)
                val device: BluetoothDevice = p1.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                val mDevice = Device("", "",0 ,"")

                if(device.name==null) {
                    mDevice.name="No Name"
                    mDevice.name="No Name"

                    Toast.makeText(applicationContext, "Devices not found", Toast.LENGTH_LONG).show()

                }
                else{
                    mDevice.name = device.name
                    mDevice.alias = device.alias!!
                }
                    Log.d("MariCarmen", "Dispositivo:"+device.type)
                    mDevice.type = device.type
                    mDevice.adress = device.address
                    devicesDiscovered.add(mDevice)
                    nRecyclerView(devicesDiscovered)



            }
            if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                Log.d("MariCarmen", "Started")

                progressVar.visibility = View.VISIBLE
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                Log.d("MariCarmen", "Finish")
                progressVar.visibility = View.GONE

            }



            }
    }



    private val bluetoothStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            var estadoConectado = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)

            Log.d("MariCarmen", "estado " + estadoConectado)

            when (estadoConectado) {

                BluetoothAdapter.STATE_OFF -> {

                    Log.d("MariCarmen", "off")
                    bluetoothStateText("Bluetooth is not aviable", false)


                }

                BluetoothAdapter.STATE_ON -> {
                    bluetoothStateText("Bluetooth is aviable", true)
                    Log.d("MariCarmen", "on")
                }

            }

        }
    }





    private fun permissionsText(text: String, validado: Boolean) {
        permissionsState.text = text

        if (validado) {
            permissionsState.setTextColor(resources.getColor(R.color.aviable))

        } else {
            permissionsState.setTextColor(resources.getColor(R.color.Disable))


        }
    }

    private fun bluetoothStateText(textState: String, validado: Boolean) {
        bluetoothStatus.text = textState

        if (validado) {
            bluetoothStatus.setTextColor(resources.getColor(R.color.aviable))
            bluetoothStatus.text = "Bluetooth is aviable"
            bluetoothImage.setImageResource(R.drawable.ic_baseline_bluetooth_connected_24)
            bluetoothImage.setColorFilter(R.color.btoOn)




        } else {
            bluetoothImage.setImageResource(R.drawable.ic_baseline_bluetooth_disabled_24)
            bluetoothImage.setColorFilter(R.color.Disable)
            bluetoothStatus.setTextColor(resources.getColor(R.color.Disable))

        }
    }


    private fun nRecyclerView(mDevices: ArrayList<Device>) {
        devicesAdapter = DevicesAdapter(mDevices)
        mRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
        }

        mRecyclerView.adapter = devicesAdapter

        devicesAdapter.setOnItemClickListener(object: DevicesAdapter.OnClickListener{
            override fun onItemClick(position: Int) {
            }
        })


    }

}



