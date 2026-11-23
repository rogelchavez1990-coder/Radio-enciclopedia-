package cu.radioenciclopedia.app

data class FmCoverage(val province: String, val mhz: String)

/** Relación nacional facilitada por el propietario el 11 de noviembre de 2026. */
object StationFrequencies {
    val all = listOf(
        FmCoverage("Pinar del Río", "102,7"),
        FmCoverage("La Habana", "94,1"),
        FmCoverage("Artemisa", "100,7"),
        FmCoverage("Matanzas", "91,1 y 106,3"),
        FmCoverage("Cienfuegos", "90,3"),
        FmCoverage("Villa Clara", "92,5 y 102,3"),
        FmCoverage("Sancti Spíritus", "91,1"),
        FmCoverage("Ciego de Ávila", "95,3"),
        FmCoverage("Camagüey", "100,5"),
        FmCoverage("Las Tunas", "101,1"),
        FmCoverage("Holguín", "102,7"),
        FmCoverage("Granma", "93,7 y 94,1"),
        FmCoverage("Santiago de Cuba", "98,1"),
        FmCoverage("Guantánamo", "96,3 y 97,1"),
        FmCoverage("Isla de la Juventud", "92,7")
    )
}
