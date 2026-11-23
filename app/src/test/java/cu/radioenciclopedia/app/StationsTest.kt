package cu.radioenciclopedia.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class StationsTest {

    @Test fun hayDiecisieteEmisorasConIdentificadoresUnicos() {
        assertEquals(17, Stations.all.size)
        assertEquals(17, Stations.all.map { it.id }.toSet().size)
    }

    @Test fun cadaEmisoraTieneSenalNombreYColores() {
        for (s in Stations.all) {
            assertTrue("sin señal: ${s.id}", s.streams.isNotEmpty())
            assertTrue("señal no válida en ${s.id}", s.streams.all { it.startsWith("https://") || it.startsWith("http://") })
            assertTrue(s.name.isNotBlank() && s.tagline.isNotBlank() && s.profile.isNotBlank() && s.mark.isNotBlank())
        }
        assertEquals(17, Stations.all.map { it.streams.first() }.toSet().size)
    }

    @Test fun soloIpnUsaHttpSinCifrar() {
        val claras = Stations.all.filter { s -> s.streams.any { it.startsWith("http://") } }.map { it.id }
        assertEquals(listOf("ipn"), claras)
    }

    @Test fun grupos() {
        assertEquals(8, Stations.inPlace(Place.HABANA).size)
        assertEquals(6, Stations.inPlace(Place.MEXICO).size)
        assertEquals(3, Stations.inPlace(Place.ESPANA).size)
    }

    @Test fun elDialSoloTieneEmisorasHabanerasConFrecuenciaFm() {
        assertTrue(Stations.onDial.all { it.place == Place.HABANA && it.fm != null })
        assertTrue(Stations.onDial.all { it.fm!! in 88f..108f })
        assertEquals(Stations.onDial.size, Stations.onDial.map { it.fm }.toSet().size)
        assertEquals(6, Stations.onDial.size)
    }

    @Test fun rebeldeTieneSenalesDistintasEnAmYFm() {
        assertEquals("https://icecast.teveo.cu/kHKL7tWd", Stations.REBELDE.stream)
        assertEquals("https://icecast.teveo.cu/zrXXWK9F", Stations.REBELDE_FM.stream)
        assertEquals(null, Stations.REBELDE.fm)
        assertEquals(96.7f, Stations.REBELDE_FM.fm)
    }

    @Test fun buscarPorIdentificador() {
        assertSame(Stations.REBELDE_FM, Stations.byId("rebelde-fm"))
        assertSame(Stations.ENCICLOPEDIA, Stations.byId("no-existe"))
        assertSame(Stations.ENCICLOPEDIA, Stations.byId(null))
        assertNotNull(Stations.byId("rne5"))
    }

    @Test fun nombresHabladosSinRepetir() {
        assertEquals("Radio Rebelde AM", Stations.REBELDE.spoken)
        assertEquals("Radio Progreso, 90,3 FM", Stations.PROGRESO.spoken)
    }
}
