package es.uniovi.eii.buscarciudades

import android.graphics.Color
import android.graphics.Color.red
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import es.uniovi.eii.sdm.buscarciudadeskot.datos.GestorCiudades
import kotlinx.android.synthetic.main.activity_maps.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var postCiudad: LatLng
    val gc = GestorCiudades()
    private lateinit var marcadorUsuario: Marker
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        botonAceptar.setOnClickListener {
            clickAceptar()
        }
        botonSiguiente.setOnClickListener {
            clickSiguiente()
        }

    }

    fun clickAceptar() {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(postCiudad, 8f))
        mMap.addMarker(
            MarkerOptions().position(postCiudad)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.estrella32))
                .title("Posiscion Real")
        )
        for (i in 1..3) {
            mMap.addCircle(
                CircleOptions().center(postCiudad).radius(30000.0 * i).fillColor(0x000ff00)
                    .strokeColor(0xff0000ff.toInt())
            )
        }
        mMap.addPolyline(
            PolylineOptions().add(postCiudad, marcadorUsuario.position).color(Color.RED)
        )
    }

    fun clickSiguiente() {
        mMap.clear()
        mapaInical()
    }

    fun controlGestos() {
        var controles = mMap.getUiSettings()
        controles.isZoomControlsEnabled = false
        controles.isZoomGesturesEnabled = false
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        controlGestos()
        mapaInical()
        mMap.setOnMapLongClickListener {
            if (::marcadorUsuario.isInitialized) {
                marcadorUsuario.remove()
            }
            val OpcionesMarca =
                MarkerOptions().position(it).title("Marcador creado por el usuario")
            mMap.addMarker(OpcionesMarca)
        }
    }

    private fun mapaInical() {
        val centro = LatLng(40.350911, -3.272910)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(centro, 5.2f))
        mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
        val ciudad = gc.nextCiudad()
        if (ciudad != null) {
            campoCiudad.text = ciudad.nombre
            postCiudad = LatLng(ciudad.latitud, ciudad.longitud)
        }
    }
}