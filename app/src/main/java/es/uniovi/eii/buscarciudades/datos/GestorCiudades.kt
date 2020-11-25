package es.uniovi.eii.sdm.buscarciudadeskot.datos

import android.util.Log
import java.util.*
import kotlin.collections.ArrayList

class GestorCiudades {
    val rnd= Random()
    val ciudades= ArrayList<Ciudad>()
    private var contCiudades= 0 // ¿Cuántas ciudades quedán?

    init {
        crearCiudades()
        contCiudades= ciudades.size
        Collections.shuffle(ciudades)
    }

    /**
     * Crea ciudades y las mete en el Gestor para luego ir recuperándolas
     */
    private fun crearCiudades() {
        addCiudad(Ciudad("La Coruña", 43.362437, -8.411027))
        addCiudad(Ciudad("Málaga", 36.725547, -4.420937))
        addCiudad(Ciudad("Zamora", 41.504773, -5.746081))
        addCiudad(Ciudad("Ciudad Real", 38.979611, -3.929980))
        addCiudad(Ciudad("Soria", 41.766639, -2.479112))
        addCiudad(Ciudad("Madrid", 40.416439, -3.704607))
        addCiudad(Ciudad("Sevilla", 37.389097, -5.984459))
        addCiudad(Ciudad("Barcelona", 41.386974, 2.169983))
        addCiudad(Ciudad("Bilbao", 43.265688, -2.935108))
        addCiudad(Ciudad("Cuenca", 40.069710, -2.136473))
        addCiudad(Ciudad("Murcia", 37.994459, -1.128714))
        addCiudad(Ciudad("Albacete", 38.993190, -1.858014))
    }

    fun addCiudad(c: Ciudad) {
        ciudades.add(c)
    }

    @Throws(NoSuchElementException::class)
    fun nextCiudad(): Ciudad? {
        if (contCiudades == 0) {
            Log.e("GestorCiudades", "nextCiudad: No quedan más ciudades que devolver")
            throw NoSuchElementException()
//            return null
        }

        contCiudades--
        return ciudades[contCiudades]
    }

    fun ciudadesQuedan() = contCiudades

    fun reiniciarCiudades():Int {
        Collections.shuffle(ciudades)
        return ciudades.size
    }

}

fun main() {
    val ciudades= GestorCiudades()

    while (ciudades.ciudadesQuedan()>0) {
        println(ciudades.nextCiudad())
    }

}