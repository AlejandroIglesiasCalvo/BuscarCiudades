package es.uniovi.eii.buscarciudades

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_INDEFINITE
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.data.kml.KmlLayer
import es.uniovi.eii.buscarciudades.BuildConfig.APPLICATION_ID
import es.uniovi.eii.sdm.buscarciudadeskot.datos.GestorCiudades
import kotlinx.android.synthetic.main.activity_maps.*
import java.io.IOException
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var aquiEstoy: LatLng
    private lateinit var mMap: GoogleMap
    private lateinit var postCiudad: LatLng
    val gc = GestorCiudades()
    private lateinit var marcadorUsuario: Marker
    private val TAG = "MapActivity"
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 34
    private lateinit var clientes: FusedLocationProviderClient
    private var postMostrada = false
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
        botonMostarPos.setOnClickListener { clickMostrarPos() }
        clientes = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun clickMostrarPos() {
        if (!postMostrada) {
            mMap.addMarker(
                MarkerOptions().position(aquiEstoy)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marcador_verdep))
                    .title("Aqui estoy")
            )
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(aquiEstoy, 8f))
            postMostrada = true
            botonMostarPos.text = "Quitar pos"
        } else {
            mapaInical()
            mMap.clear()
            postMostrada = false
            botonMostarPos.text = "Mostar pos"
        }

    }

    override fun onStart() {
        super.onStart()
        if (!checkPermissions()) {
            requestPermissions()
        } else {
            getLastLocation()
        }
    }
    /*****************************************************************************************
     * Gestión de permisos
     *****************************************************************************************/

    /** Devuelve el estado actual del permiso ACCESS_COARSE_LOCATION   */
    private fun checkPermissions() =
        ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED

    /** Gestiona la solicitud de permisos */
    private fun requestPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, ACCESS_COARSE_LOCATION)) {
            // Proporciona una explicación adicional al usuario. Esto podría pasar si el usuario
            // ha denegado la petición previamente, pero no marca el checkbox de "No preguntar de nuevo"
            Log.i(TAG, "Muestra la explicación de la necesidad del permiso.")
            showSnackbar(R.string.permission_rationale, android.R.string.ok, View.OnClickListener {
                // Solicita permiso
                startLocationPermissionRequest()
            })

        } else {
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            Log.i(TAG, "Solicitud de permiso")
            startLocationPermissionRequest()
        }
    }

    /** Lanza la petición de permisos ACCESS_COARSE_LOCATION */
    private fun startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(
            this, arrayOf(ACCESS_COARSE_LOCATION),
            REQUEST_PERMISSIONS_REQUEST_CODE
        )
    }

    /** Gestina la aprobación o denegación del permiso por parte del usuario */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            when {
                // Si la interacción del usuario fue interrumpida, la petición del permiso se
                // cancela y recibe un array vacío
                grantResults.isEmpty() -> Log.i(TAG, "La interacción del usuario fue cancelada.")

                // Permiso concedido
                (grantResults[0] == PackageManager.PERMISSION_GRANTED) -> getLastLocation()

                // Permiso denegado
                else -> {
                    // Notifica al usuario via SnackBar que ha denegado un permiso clave para la aplicación,
                    // lo cual hace que la activity carezca de sentido. En una app real, los permisos clave
                    // debe solicitarse en una pantalla de bienvenida

                    // Adicionalmente, es importante recordar que un permiso puede ser denegado
                    // sin preguntarle al usuario (por política del dispositivo o que previamente
                    // haya marcado "no volver a preguntar"). Por tanto, cuando los permisos se
                    // deniegan se implementa una interfaz para ofrecer al usuario que los cambie.
                    // Si no es así la app podría parecer que la app no responde a los toques o
                    // interacciones que tienen permisos requeridos.
                    showSnackbar(R.string.permission_denied_explanation, R.string.settings,
                        View.OnClickListener {
                            // Construye un intent que muestra la pantalla de settings de la App
                            val intent = Intent().apply {
                                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                data = Uri.fromParts("package", APPLICATION_ID, null)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(intent)
                        })
                }
            }
        }
    }


    /**
     * Muestra una snackbar
     *
     * @param snackStrId El id del recurso con el texto de la snackbar
     * @param actionStrId Texto del action item
     * @param listener El
     */
    private fun showSnackbar(
        snackStrId: Int,
        actionStrId: Int = 0,
        listener: View.OnClickListener? = null
    ) {
        val snackbar = Snackbar.make(
            findViewById(android.R.id.content), getString(snackStrId),
            LENGTH_INDEFINITE
        )
        if (actionStrId != 0 && listener != null) {
            snackbar.setAction(getString(actionStrId), listener)
        }
        snackbar.show()
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
        val distancia = listOf(0.0f).toFloatArray()
        Location.distanceBetween(
            postCiudad.latitude,
            postCiudad.longitude,
            marcadorUsuario.position.latitude,
            marcadorUsuario.position.longitude,
            distancia
        )
        mostrarDistancia(distancia[0])
    }

    /**
     * Recupera la dirección desde el servicio de mapas
     */
    private fun getDireccion(location: LatLng): String {
        var errorMessage = ""
        var direccion = ""
        // Podría haber varios tipos de errores usando el Geocoder (por ejemplo, si no hay
        // conexión, o si se envian datos incorrectos). También podría pasar que el Geocoder
        // no tenga ninguna dirección para esa ubicación. En todos estos casos, recibimos un
        // resultCode indicando el fallo. Si se encuentra una dirección, recibimos un resultCode
        // indicando éxito

        // The Geocoder used in this sample. The Geocoder's responses are localized for the given
        // Locale, which represents a specific geographical or linguistic region. Locales are used
        // to alter the presentation of information such as numbers or dates to suit the conventions
        // in the region they describe.
        val geocoder = Geocoder(this, Locale.getDefault())

        // Address found using the Geocoder.
        var addresses: List<Address> = emptyList()

        try {
            // Using getFromLocation() returns an array of Addresses for the area immediately
            // surrounding the given latitude and longitude. The results are a best guess and are
            // not guaranteed to be accurate.
            addresses = geocoder.getFromLocation(
                location.latitude,
                location.longitude,
                // In this sample, we get just a single address.
                1
            )
        } catch (ioException: IOException) {
            // Catch network or other I/O problems.
            errorMessage = getString(R.string.service_not_available)
            Log.e(ContentValues.TAG, errorMessage, ioException)
        } catch (illegalArgumentException: IllegalArgumentException) {
            // Catch invalid latitude or longitude values.
            errorMessage = getString(R.string.invalid_lat_long_used)
            Log.e(
                ContentValues.TAG, "$errorMessage. Latitude = $location.latitude , " +
                        "Longitude = $location.longitude", illegalArgumentException
            )
        }

        // Handle case where no address was found.
        if (addresses.isEmpty()) {
            if (errorMessage.isEmpty()) {
                errorMessage = getString(R.string.no_address_found)
                Log.e(ContentValues.TAG, errorMessage)
            }
            direccion = errorMessage
        } else {
            val address = addresses[0]
            // Fetch the address lines using {@code getAddressLine},
            // join them, and send them to the thread. The {@link android.location.address}
            // class provides other options for fetching address details that you may prefer
            // to use. Here are some examples:
            // getLocality() ("Mountain View", for example)
            // getAdminArea() ("CA", for example)
            // getPostalCode() ("94043", for example)
            // getCountryCode() ("US", for example)
            // getCountryName() ("United States", for example)
            val addressFragments = with(address) {
                (0..maxAddressLineIndex).map { getAddressLine(it) }
            }

            Log.i(ContentValues.TAG, getString(R.string.address_found))
            direccion =
                addressFragments.joinToString(separator = "\n")
        }
        return direccion
    }

    /*****************************************************************************************
     * Gestión geolocalización (previamente creamos el cliente y gestionamos permisos)
     *****************************************************************************************/

    /**
     * Obtiene la mejor y más reciente ubicación disponible actualmente,
     * que puede ser nula en casos excepcionales cuando una ubicación no está disponible.
     */
    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        clientes.lastLocation
            .addOnCompleteListener { taskLocation ->
                if (taskLocation.isSuccessful && taskLocation.result != null) {
                    Log.d(ContentValues.TAG, "leemos localizacion gps", taskLocation.exception)
                    val location = taskLocation.result
                    if (location != null) {
                        aquiEstoy = LatLng(location.latitude, location.longitude)
                    }
                } else {
                    Log.w(ContentValues.TAG, "getLastLocation:exception", taskLocation.exception)
                    showSnackbar(R.string.no_location_detected)
                }
            }
    }

    private fun mostrarDistancia(distancia: Float) {
        val builder = AlertDialog.Builder(this@MapsActivity)

        builder.setMessage("La distancia al punto real es: $distancia")//R.string.dialog_message)
            .setTitle("Distancia")//R.string.dialog_title)
        val dialog = builder.create()
        dialog.show()
    }

    fun clickSiguiente() {
        mMap.clear()
        mapaInical()
    }

    fun controlGestos() {
        var controles = mMap.uiSettings
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
        val layer = KmlLayer(mMap, R.raw.comunidades_autonomas_espanolas, this)
        layer.addLayerToMap()
    }
}