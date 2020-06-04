package org.personal.coupleapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnSuccessListener
import kotlinx.android.synthetic.main.activity_map.*

import java.io.IOException


class MapActivity : AppCompatActivity(), View.OnClickListener, OnMapReadyCallback, SearchView.OnQueryTextListener,
    OnSuccessListener<Location?> {

    private val TAG = javaClass.name

    private var customMap: GoogleMap? = null
    private var hospitalLocation: String? = null
    private var currentLocation: Location? = null
    private var currentLatLng: LatLng? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        setListener()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationPermission()
        (mapFragment as SupportMapFragment).getMapAsync(this)
    }

    private fun setListener() {

    }

    override fun onClick(v: View) {
        when (v.id) {

        }
    }

    override fun onBackPressed() {
        val backButton = Intent(this, MainHomeActivity::class.java)
        startActivity(backButton)
        super.onBackPressed()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        customMap = googleMap
        customMap!!.mapType = GoogleMap.MAP_TYPE_NORMAL
        if (hospitalLocation != "") {
            val handler = Handler()
            handler.postDelayed({
            }, 1000)
        }
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        val location = searchLocationSV!!.query.toString()
        findLocation(location)
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        return false
    }

    private fun locationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED
            || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED
        ) {
            val permission = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            ActivityCompat.requestPermissions(this, permission, FINE_LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            fetchLastLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLastLocation() {
        Log.i(TAG, "지도 테스트 : yes")
        val task = fusedLocationProviderClient.lastLocation
        task.addOnSuccessListener(this)
    }

    override fun onSuccess(location: Location?) {
        if (location != null) {
            currentLocation = location
            currentLatLng = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)
            val markerOptions = MarkerOptions().position(currentLatLng!!).title("내 위치")
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.my_current_location))
            customMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
            customMap!!.addMarker(markerOptions)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            FINE_LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        if (grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                            fetchLastLocation()
                        }
                    }
                }
            }
        }
    }

    private fun findLocation(location: String?) {
        var addressList: List<Address>? = null
        if (location != "") {
            val geocoder = Geocoder(this)
            try {
                addressList = geocoder.getFromLocationName(location, 5)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            if (addressList != null) {
                if (addressList.isNotEmpty()) {
                    val address = addressList[0]
                    val latlng = LatLng(address.latitude, address.longitude)
                    customMap!!.addMarker(MarkerOptions().position(latlng).title(location))
                    customMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 16f))
                } else {
                    Toast.makeText(this, "다시 검색해 주세요", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val FINE_LOCATION_PERMISSION_REQUEST_CODE = 10
    }
}
