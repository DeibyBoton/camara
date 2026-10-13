package com.xperiaprocam.domain.model

/** Camera capture modes — only modes that really work on this codebase. */
enum class CaptureMode(val label: String) {
    CLASSIC("Foto"),
    NIGHT("Noche"),            // 6 fotos apiladas
    HDR("HDR"),                // horquillado EV -2/0/+2 y fusión ponderada
    PRO("Pro"),                // ISO/obturador/enfoque/WB manuales
    HIGHRES("Alta Res."),      // resolución máxima JPEG del sensor
    MACRO("Macro"),            // zoom mínimo (ultra gran angular) + enfoque cercano
    PANORAMA("Panorámica"),    // barrido + costura
    FOOD("Comida"),            // perfil cálido post-captura
    STARS("Estrellas"),        // astro: exposición manual + apilado
    VIDEO("Video");            // calidad/fps seleccionables

    val isSpecialPhoto: Boolean
        get() = this in listOf(NIGHT, STARS, HDR, PANORAMA, FOOD, MACRO, HIGHRES)

    val isPrimary: Boolean
        get() = this in listOf(CLASSIC, VIDEO, PRO)

    val burstFrames: Int
        get() = when (this) {
            NIGHT -> 6
            HDR -> 3
            STARS -> 12
            else -> 1
        }

    val fileSuffix: String?
        get() = when (this) {
            NIGHT -> "NIGHT"
            HDR -> "HDR"
            STARS -> "STARS"
            FOOD -> "FOOD"
            PANORAMA -> "PANO"
            else -> null
        }
}

