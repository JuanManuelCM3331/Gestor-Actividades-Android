package com.example.labo_android_semana_02.ui.navigation

object AppRoutes {
    const val WELCOME = "welcome"
    const val ACTIVITIES = "activities"
    const val REPORTE_FORM_PATTERN = "activities/form?reporteId={reporteId}"
    const val REPORTE_DETAIL_PATTERN = "activities/{reporteId}"
    const val ACTIVIDAD_DETAIL_PATTERN = "remoto/{actividadId}"

    fun reporteForm(reporteId: Int? = null): String =
        if (reporteId != null && reporteId != 0) "activities/form?reporteId=$reporteId"
        else "activities/form"

    fun reporteDetail(reporteId: Int): String = "activities/$reporteId"

    fun actividadDetail(actividadId: Int): String = "remoto/$actividadId"
}
