package org.personal.coupleapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main_map.*

import java.io.IOException


class MainMapActivity : AppCompatActivity(), View.OnClickListener, OnMapReadyCallback, SearchView.OnQueryTextListener,
    OnSuccessListener<Location?>, BottomNavigationView.OnNavigationItemSelectedListener {

    private val TAG = javaClass.name

    // 위치 찾는 퍼미션 실행 Request Code
    private val FINE_LOCATION_PERMISSION_REQUEST_CODE = 10

    private var customMap: GoogleMap? = null
    private var currentLocation: Location? = null
    private var currentLatLng: LatLng? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_map)
        setListener()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationPermission()
        (mapFragment as SupportMapFragment).getMapAsync(this)
    }

    override fun onResume() {
        super.onResume()
        bottomNavigation.selectedItemId = R.id.map
    }

    // 클릭 리스너 setting 하는 메소드
    private fun setListener() {
        bottomNavigation.setOnNavigationItemSelectedListener(this)
        myLocationBtn.setOnClickListener(this)
        searchLocationSV.setOnQueryTextListener(this)
    }

    // 초반 구글 맵을 불러오면서 위치 관련 permission 을 받는다
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

    //------------------ 네비게이션 바 클릭 시 이벤트 관리하는 메소드 모음 ------------------
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home ->  toHome()
            R.id.chat -> toChat()
            R.id.album -> toAlbum()
            R.id.profile -> toProfile()
        }
        overridePendingTransition(0, 0)
        return true
    }

    private fun toHome() {
        val toMap = Intent(this, MainHomeActivity::class.java)
        startActivity(toMap)
    }

    private fun toChat() {
        val toChat = Intent(this, MainChatActivity::class.java)
        startActivity(toChat)
    }

    private fun toAlbum() {
        val toAlbum = Intent(this, MainAlbumActivity::class.java)
        startActivity(toAlbum)
    }

    private fun toProfile() {
        val toMore = Intent(this, MainProfileActivity::class.java)
        startActivity(toMore)
    }

    //------------------ 버튼 클릭 시 이벤트 관리하는 메소드 모음 ------------------
    override fun onClick(v: View) {
        when (v.id) {
            R.id.myLocationBtn -> fetchLastLocation()
        }
    }

    // 구글 맵 불러오는 callback 메소드
    override fun onMapReady(googleMap: GoogleMap) {
        customMap = googleMap
        customMap!!.mapType = GoogleMap.MAP_TYPE_NORMAL
    }

    // searchLocationSV 찾고 싶은 위치를 검색했을 때 위치를 불러오는 callback 메소드
    override fun onQueryTextSubmit(query: String): Boolean {
        val location = searchLocationSV!!.query.toString()
        findLocation(location)
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        return false
    }

    // 본인의 최근 위치를 찾아서 구글 맵에 표시한다
    @SuppressLint("MissingPermission")
    private fun fetchLastLocation() {
        Log.i(TAG, "지도 테스트 : yes")
        val task = fusedLocationProviderClient.lastLocation
        task.addOnSuccessListener(this)
    }

    // 위치를 찾았을 때 마커를 추가해주는 callback 메소드
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

    // permission callback 메소드
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
}
