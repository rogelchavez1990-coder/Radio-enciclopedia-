package cu.radioenciclopedia.app

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

data class RadioProgram(val id: String, val title: String, val description: String)

/** Se publican horas de INICIO; no se presenta 09:69 del original como una hora válida. */
data class Broadcast(val startMinute: Int, val endMinute: Int, val programId: String, val replay: Boolean = false) {
    fun time(): String = "%02d:%02d".format(startMinute / 60, startMinute % 60)
}

object ProgramSchedule {
    val zone: ZoneId = ZoneId.of("America/Havana")
    const val SOURCE = "https://www.radioenciclopedia.cu/programacion-general/"
    const val REVIEWED = "11 noviembre 2026"
    val programs = listOf(
        RadioProgram("interludio", "Interludio", "Música instrumental, compositores, intérpretes y efemérides musicales."),
        RadioProgram("silencio", "En la intimidad del silencio", "El espacio musical de la madrugada de Radio Enciclopedia."),
        RadioProgram("cita", "Cita en Enciclopedia", "Historia, cultura, biografías, efemérides y curiosidades."),
        RadioProgram("amanecer", "Al Amanecer", "El espacio diario que abre el tramo matutino de la emisora."),
        RadioProgram("aurora", "Hola Aurora", "Revista informativa en vivo sobre la actualidad y el acontecer cultural."),
        RadioProgram("album", "Álbum de melodías", "Música y piezas solicitadas por la audiencia."),
        RadioProgram("gotas", "Gotas del saber", "Ciencia, tecnología, medioambiente, salud y astronomía."),
        RadioProgram("media", "Media hora con su intérprete", "Retratos musicales de intérpretes, agrupaciones y tradiciones."),
        RadioProgram("contigo", "Contigo siempre", "La revista cultural de la tarde."),
        RadioProgram("lugar", "Un lugar en el aire", "Géneros musicales, autores, intérpretes y formatos."),
        RadioProgram("tiempo", "Más allá de un tiempo", "Historias, curiosidades y lugares de distintas épocas."),
        RadioProgram("hablemos", "Hablemos simplemente", "Curiosidades, ciencia, geografía, artes, historia y literatura con música."),
        RadioProgram("ultima", "La última hora del día", "Reflexiones sobre la amistad, la vida y figuras de la cultura universal."),
        RadioProgram("secreto", "Un secreto para ti", "Reflexiones sobre bienestar, crecimiento personal y vida cotidiana."),
        RadioProgram("poesia", "La poesía toda", "Lectura y comentario de poesía cubana y universal.")
    )
    private val byId = programs.associateBy { it.id }
    fun program(slot: Broadcast): RadioProgram = byId.getValue(slot.programId)

    // Los minutos de identificación del original quedan fuera de los espacios principales.
    private val weekday = listOf(
        Broadcast(0, 59, "interludio"), Broadcast(60, 299, "silencio"),
        Broadcast(300, 359, "cita"), Broadcast(360, 419, "amanecer"),
        Broadcast(420, 539, "aurora"), Broadcast(540, 599, "interludio", true),
        Broadcast(600, 659, "cita", true), Broadcast(660, 779, "album"),
        Broadcast(780, 839, "gotas"), Broadcast(840, 869, "media"),
        Broadcast(870, 1019, "contigo"), Broadcast(1020, 1139, "lugar"),
        Broadcast(1140, 1199, "gotas", true), Broadcast(1200, 1319, "tiempo"),
        Broadcast(1320, 1379, "hablemos"), Broadcast(1380, 1439, "ultima")
    )
    private val sunday = listOf(
        Broadcast(0, 60, "interludio"), Broadcast(60, 360, "silencio"),
        Broadcast(360, 420, "amanecer"), Broadcast(420, 450, "media"),
        Broadcast(450, 510, "lugar"), Broadcast(510, 540, "secreto"),
        Broadcast(540, 600, "interludio", true), Broadcast(600, 660, "cita"),
        Broadcast(660, 780, "album"), Broadcast(780, 840, "gotas"),
        Broadcast(840, 870, "media", true), Broadcast(870, 1020, "contigo"),
        Broadcast(1020, 1140, "lugar", true), Broadcast(1140, 1200, "gotas", true),
        Broadcast(1200, 1230, "poesia"), Broadcast(1230, 1260, "secreto", true),
        Broadcast(1260, 1320, "tiempo"), Broadcast(1320, 1380, "hablemos"),
        Broadcast(1380, 1440, "ultima")
    )

    fun day(day: DayOfWeek): List<Broadcast> = if (day == DayOfWeek.SUNDAY) sunday else weekday
    fun current(at: Instant = Instant.now()): Broadcast? {
        val local = at.atZone(zone)
        val minute = local.hour * 60 + local.minute
        return day(local.dayOfWeek).firstOrNull { minute >= it.startMinute && minute < it.endMinute }
    }
    fun currentTitle(at: Instant = Instant.now()): String = current(at)?.let { program(it).title } ?: "Continuidad de la emisora"

    fun hours(id: String): String {
        fun group(slots: List<Broadcast>) = slots.filter { it.programId == id }
            .joinToString(" · ") { it.time() + if (it.replay) " (R)" else "" }
        return listOf("Lun–sáb" to group(weekday), "Domingo" to group(sunday))
            .filter { it.second.isNotEmpty() }.joinToString("\n") { "${it.first}: ${it.second}" }
    }
}
