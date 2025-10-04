// package com.example.reportes.administrador
package com.example.reportes.ui.administrador

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.example.reportes.R
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.renderer.PieChartRenderer
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.widget.AdapterView
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Date
import android.os.Environment
import android.os.Build
import android.provider.MediaStore
import android.content.ContentValues
import android.graphics.Typeface
import android.util.Log
import android.view.MotionEvent
import androidx.activity.viewModels
import com.example.reportes.viewmodels.ViewModelAdmin.ResumenViewModel
import com.example.reportes.ui.usuario.BaseActivity
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.FillPatternType
import com.google.firebase.firestore.ListenerRegistration
import java.io.File
import java.io.FileOutputStream
import androidx.appcompat.app.AlertDialog
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.utils.ViewPortHandler
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.util.CellRangeAddress
import java.text.SimpleDateFormat

/**
 * Actividad para mostrar el resumen de reportes con gráficos y exportación de datos
 */
class ResumenActivity : BaseActivity() {

    // SECCIÓN: PROPIEDADES Y DEPENDENCIAS
    private val viewModel: ResumenViewModel by viewModels()
    private val db = FirebaseFirestore.getInstance()

    // SECCIÓN: UI COMPONENTS
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var etFechaDesde: EditText
    private lateinit var etFechaHasta: EditText
    private lateinit var spinnerTipo: Spinner

    // SECCIÓN: VARIABLES DE ESTADO
    private var fechaDesdeValue: String = ""
    private var fechaHastaValue: String = ""
    private var tipoValue: String = "Todos"

    // Control de estado de los componentes UI
    private var datePickerDesdeAbierto = false
    private var datePickerHastaAbierto = false
    private var exportDialogAbierto = false
    private var reportesListener: ListenerRegistration? = null

    // SECCIÓN: CICLO DE VIDA
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resumen)

        setupUI()
        setupCharts()
        setupObservers()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("fechaDesdeValue", etFechaDesde.text.toString())
        outState.putString("fechaHastaValue", etFechaHasta.text.toString())
        outState.putString("tipoValue", spinnerTipo.selectedItem.toString())
    }

    override fun onDestroy() {
        super.onDestroy()
        // El listener se remueve en onCleared del ViewModel
    }

    // SECCIÓN: CONFIGURACIÓN DE UI
    /**
     * Configura los componentes de interfaz de usuario y sus listeners
     */
    private fun setupUI() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        
        // Configuración del botón exportar
        val btnExportar = findViewById<Button>(R.id.btnExportar)
        btnExportar.setOnClickListener {
            // Solo permite abrir si no está abierto
            if (!exportDialogAbierto) {
                exportDialogAbierto = true
                btnExportar.isEnabled = false // Deshabilita el botón hasta que se cierre el diálogo
                viewModel.prepararDatosExportar(etFechaDesde.text.toString(), etFechaHasta.text.toString())
            }
        }

        // Configuración de campos de fecha
        etFechaDesde = findViewById(R.id.etFechaDesde)
        etFechaHasta = findViewById(R.id.etFechaHasta)

        // Deshabilitar completamente la edición de los campos
        etFechaDesde.keyListener = null
        etFechaHasta.keyListener = null

        // Listener para etFechaDesde
        etFechaDesde.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = etFechaDesde.compoundDrawables[2] // 2 es el índice para drawableEnd
                if (drawableEnd != null && event.x >= v.width - v.paddingRight - drawableEnd.intrinsicWidth) {
                    // Si el calendario no está en proceso de abrirse, ábrelo
                    if (!datePickerDesdeAbierto) {
                        datePickerDesdeAbierto = true // Bloquea inmediatamente
                        mostrarDatePickerDesde()
                    }
                    return@setOnTouchListener true // Consume el evento para evitar otros listeners
                }
            }
            true // Consume el evento para evitar que el teclado aparezca
        }

        // Listener para etFechaHasta
        etFechaHasta.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = etFechaHasta.compoundDrawables[2]
                if (drawableEnd != null && event.x >= v.width - v.paddingRight - drawableEnd.intrinsicWidth) {
                    // Si el calendario no está en proceso de abrirse, ábrelo
                    if (!datePickerHastaAbierto) {
                        datePickerHastaAbierto = true // Bloquea inmediatamente
                        mostrarDatePickerHasta()
                    }
                    return@setOnTouchListener true // Consume el evento
                }
            }
            true // Consume el evento
        }

        // Configuración del spinner de tipo
        setupSpinner()
    }

    /**
     * Configura el spinner para selección de tipo de reporte
     */
    private fun setupSpinner() {
        spinnerTipo = findViewById(R.id.spinner_tipo)

        val tiposHallazgo = listOf("Todos", "Condición insegura", "Acto inseguro")
        val adapterTipo = ArrayAdapter(this, R.layout.spinner_item, tiposHallazgo)
        adapterTipo.setDropDownViewResource(R.layout.spinner_item)
        spinnerTipo.adapter = adapterTipo
        spinnerTipo.setSelection(0)

        spinnerTipo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                tipoValue = spinnerTipo.selectedItem.toString()
                viewModel.actualizarGraficos(etFechaDesde.text.toString(), etFechaHasta.text.toString(), tipoValue)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    // SECCIÓN: CONFIGURACIÓN DE GRÁFICOS
    /**
     * Configura los gráficos iniciales
     */
    private fun setupCharts() {
        pieChart = findViewById(R.id.pieChart)
        barChart = findViewById(R.id.barChart)
        setupPieChart()
        setupBarChart()
    }

    /**
     * Configura el gráfico de tipo pie (torta)
     */
    private fun setupPieChart() {
        // Datos del gráfico
        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(40f, "Pendiente"))
        entries.add(PieEntry(20f, "Asignado"))
        entries.add(PieEntry(20f, "Cerrado"))
        entries.add(PieEntry(20f, "Cerrado parcial")) // Nuevo estado

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            Color.YELLOW, // Pendiente
            Color.parseColor("#2946A4"), // Asignado
            Color.parseColor("#00FF00"), // Cerrado
            Color.parseColor("#FFA500")  // Cerrado parcial (naranja)
        )
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 16f
        dataSet.setDrawValues(false)
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                return "${value.toInt()}%"
            }
        }

        val pieData = PieData(dataSet)
        pieChart.data = pieData
        pieChart.setUsePercentValues(false)
        pieChart.setDrawEntryLabels(false)
        pieChart.description.isEnabled = false
        pieChart.setDrawCenterText(false)
        pieChart.setDrawHoleEnabled(false)
        pieChart.setExtraOffsets(0f, 0f, 0f, 0f)
        pieChart.isClickable = false
        pieChart.setHighlightPerTapEnabled(false)

        // Leyenda horizontal debajo del título
        val legend = pieChart.legend
        legend.isEnabled = true
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.textSize = 12f
        legend.form = Legend.LegendForm.CIRCLE
        legend.formSize = 13f
        legend.xEntrySpace = 8f
        legend.yEntrySpace = 8f

        pieChart.animateY(1000)
        pieChart.invalidate()
        pieChart.renderer = CustomPieChartRenderer(pieChart, pieChart.animator, pieChart.viewPortHandler)
    }

    /**
     * Configura el gráfico de barras
     */
    private fun setupBarChart() {
        // Configuración básica del gráfico de barras
        barChart.setDrawBarShadow(false)
        barChart.setDrawValueAboveBar(true)
        barChart.description.isEnabled = false
        barChart.setPinchZoom(false)
        barChart.setDrawGridBackground(false)
        barChart.setScaleEnabled(false)
        
        // Configurar ejes
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return when (value.toInt()) {
                    0 -> "Bajo"
                    1 -> "Medio"
                    2 -> "Alto"
                    else -> ""
                }
            }
        }
        
        // Ajustar márgenes del gráfico
        barChart.setExtraOffsets(0f, 0f, 0f, 16f) // 10px extra en la parte inferior
        
        val leftAxis = barChart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.axisMinimum = 0f
        leftAxis.granularity = 1f
        leftAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        }
        
        barChart.axisRight.isEnabled = false
        
        // Leyenda
        val legend = barChart.legend
        legend.isEnabled = false
        
        // Establecer datos iniciales
        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, 0f)) // Bajo
        entries.add(BarEntry(1f, 0f)) // Medio
        entries.add(BarEntry(2f, 0f)) // Alto
        
        val dataSet = BarDataSet(entries, "Niveles de riesgo")
        dataSet.colors = listOf(
            Color.parseColor("#00FF00"), // Verde para Bajo
            Color.parseColor("#FFC000"), // Amarillo para Medio
            Color.parseColor("#EF2121")  // Rojo para Alto
        )
        dataSet.valueTextSize = 14f
        
        val barData = BarData(dataSet)
        barData.barWidth = 0.6f
        
        barChart.data = barData
        barChart.setFitBars(true)
        barChart.animateY(1000)
        barChart.invalidate()

        // Desactivar highlight (selección) en el gráfico de barras
        barChart.isHighlightPerTapEnabled = false
        barChart.isHighlightPerDragEnabled = false
        barChart.highlightValues(null)
    }

    // SECCIÓN: OBSERVADORES Y LISTENERS
    /**
     * Configura los observadores de LiveData del ViewModel
     */
    private fun setupObservers() {
        viewModel.allReportes.observe(this) { reportes ->
            if (reportes != null) {
                val (desde, hasta) = viewModel.getFechasIniciales(reportes)
                fechaDesdeValue = desde
                fechaHastaValue = hasta
                etFechaDesde.setText(fechaDesdeValue)
                etFechaHasta.setText(fechaHastaValue)
                viewModel.actualizarGraficos(fechaDesdeValue, fechaHastaValue, spinnerTipo.selectedItem.toString())
            }
        }

        viewModel.reportesFiltrados.observe(this) { reportes ->
            if (reportes != null) {
                actualizarGrafico(reportes)
                actualizarGraficoBarras(reportes)
            }
        }

        viewModel.datosExportar.observe(this) { datos ->
            if (datos != null) {
                exportarDatosAExcel(datos)
            }
        }

        viewModel.error.observe(this) { errorMsg ->
            if (errorMsg != null) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                Log.e("ResumenActivity", errorMsg)
            }
        }
    }

    // SECCIÓN: DATEPICKERS
    /**
     * Muestra el DatePicker para seleccionar la fecha desde
     */
    private fun mostrarDatePickerDesde() {
        val sdf = SimpleDateFormat("dd/MM/yyyy")
        val calendario = Calendar.getInstance()

        try {
            val fechaTexto = etFechaDesde.text.toString()
            if (fechaTexto.isNotEmpty()) {
                calendario.time = sdf.parse(fechaTexto) ?: Date()
            }
        } catch (_: Exception) {}

        val reportes = viewModel.allReportes.value ?: emptyList()
        val fechas = reportes.mapNotNull { try { sdf.parse(it.fechacreacion) } catch (_: Exception) { null } }.sorted()
        val minDate = fechas.firstOrNull()
        val maxDate = fechas.lastOrNull()
        mostrarDatePickerDesdeConLimites(calendario, minDate, maxDate)
    }

    /**
     * Configura y muestra el DatePickerDialog con límites de fecha
     */
    private fun mostrarDatePickerDesdeConLimites(calendario: Calendar, minDate: Date?, maxDate: Date?) {
        val sdf = SimpleDateFormat("dd/MM/yyyy")
        
        if (minDate == null || maxDate == null) {
            Toast.makeText(this, "No se encontraron reportes para establecer límites de fechas", Toast.LENGTH_SHORT).show()
            datePickerDesdeAbierto = false
            return
        }
        
        val fechaActual = calendario.time
        if (fechaActual.before(minDate)) {
            calendario.time = minDate
        } else if (fechaActual.after(maxDate)) {
            calendario.time = maxDate
        }
        
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val fechaSeleccionadaCal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                val fecha = sdf.format(fechaSeleccionadaCal.time)
                etFechaDesde.setText(fecha)
                fechaDesdeValue = fecha
                
                // --- LÓGICA AUTOMÁTICA PARA FECHA HASTA ---
                val calMaxDate = Calendar.getInstance().apply { time = maxDate }
                
                // Si el mes seleccionado es el mismo que el del último reporte
                if (fechaSeleccionadaCal.get(Calendar.YEAR) == calMaxDate.get(Calendar.YEAR) &&
                    fechaSeleccionadaCal.get(Calendar.MONTH) == calMaxDate.get(Calendar.MONTH)) {
                    // Poner la fecha del último reporte
                    fechaHastaValue = sdf.format(maxDate)
                } else {
                    // Si es un mes anterior, poner fin de mes del mes seleccionado
                    fechaSeleccionadaCal.set(Calendar.DAY_OF_MONTH, fechaSeleccionadaCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                    fechaHastaValue = sdf.format(fechaSeleccionadaCal.time)
                }
                
                etFechaHasta.setText(fechaHastaValue)
                viewModel.actualizarGraficos(fechaDesdeValue, fechaHastaValue, spinnerTipo.selectedItem.toString())
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.datePicker.minDate = minDate.time
        datePicker.datePicker.maxDate = maxDate.time
        
        datePicker.setOnDismissListener {
            datePickerDesdeAbierto = false
        }
        
        datePicker.show()
    }

    /**
     * Muestra el DatePicker para seleccionar la fecha hasta
     */
    private fun mostrarDatePickerHasta() {
        val sdf = SimpleDateFormat("dd/MM/yyyy")
        val calendario = Calendar.getInstance()

        try {
            val fechaTexto = etFechaHasta.text.toString()
            if (fechaTexto.isNotEmpty()) {
                calendario.time = sdf.parse(fechaTexto) ?: Date()
            }
        } catch (_: Exception) {}

        val desdeTexto = etFechaDesde.text.toString()
        var desdeDate: Date? = null

        try {
            if (desdeTexto.isNotEmpty()) {
                desdeDate = sdf.parse(desdeTexto)
            }
        } catch (_: Exception) {}

        if (desdeDate == null) {
            Toast.makeText(this, "Primero seleccione una fecha 'Desde'", Toast.LENGTH_SHORT).show()
            datePickerHastaAbierto = false
            return
        }

        val reportes = viewModel.allReportes.value ?: emptyList()
        val fechas = reportes.mapNotNull { try { sdf.parse(it.fechacreacion) } catch (_: Exception) { null } }.sorted()
        val maxDate = fechas.lastOrNull()

        if (maxDate == null) {
            Toast.makeText(this, "No se pudo determinar la fecha máxima", Toast.LENGTH_SHORT).show()
            datePickerHastaAbierto = false
            return
        }

        if (calendario.time.before(desdeDate)) {
            calendario.time = desdeDate
        } else if (calendario.time.after(maxDate)) {
            calendario.time = maxDate
        }

        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val fechaSeleccionada = Calendar.getInstance()
                fechaSeleccionada.set(year, month, dayOfMonth)

                val fecha = "%02d/%02d/%04d".format(dayOfMonth, month + 1, year)
                etFechaHasta.setText(fecha)
                fechaHastaValue = fecha
                viewModel.actualizarGraficos(fechaDesdeValue, fechaHastaValue, spinnerTipo.selectedItem.toString())
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.datePicker.minDate = desdeDate.time
        datePicker.datePicker.maxDate = maxDate.time

        datePicker.setOnDismissListener {
            datePickerHastaAbierto = false
        }

        datePicker.show()
    }

    // SECCIÓN: ACTUALIZACIÓN DE GRÁFICOS
    /**
     * Actualiza los gráficos con los nuevos filtros
     * @deprecated Esta función es redundante y se llama directamente al ViewModel
     */
    private fun actualizarGraficos(fechaDesde: String, fechaHasta: String, tipo: String) {
        // La llamada ahora se hace directamente al ViewModel
        viewModel.actualizarGraficos(fechaDesde, fechaHasta, tipo)
    }

    /**
     * Actualiza el gráfico de torta con los nuevos datos
     */
    private fun actualizarGrafico(reportes: List<com.example.reportes.data.model.Reporte>) {
        val tvTituloGrafico = findViewById<TextView>(R.id.tvTituloGrafico)
        tvTituloGrafico.text = when (tipoValue) {
            "Todos" -> "Todos los reportes"
            else -> tipoValue
        }

        var pendientes = 0
        var asignados = 0
        var cerrados = 0
        var cerradosParcial = 0

        for (reporte in reportes) {
            when {
                reporte.estado.equals("Pendiente", ignoreCase = true) -> pendientes++
                reporte.estado.equals("Asignado", ignoreCase = true) -> asignados++
                reporte.estado.equals("Cerrado", ignoreCase = true) -> cerrados++
                reporte.estado.equals("Cerrado parcial", ignoreCase = true) -> cerradosParcial++
            }
        }

        val total = pendientes + asignados + cerrados + cerradosParcial
        val entries = ArrayList<PieEntry>()

        val pPendientes = if (total > 0) pendientes * 100f / total else 0f
        val pAsignados = if (total > 0) asignados * 100f / total else 0f
        val pCerrados = if (total > 0) cerrados * 100f / total else 0f
        val pCerradosParcial = if (total > 0) cerradosParcial * 100f / total else 0f

        entries.add(PieEntry(pPendientes, "Pendiente"))
        entries.add(PieEntry(pAsignados, "Asignado"))
        entries.add(PieEntry(pCerrados, "Cerrado"))
        entries.add(PieEntry(pCerradosParcial, "Cerrado parcial"))

        actualizarPieChart(entries)
    }
    
    /**
     * Actualiza el gráfico de barras con los nuevos datos
     */
    private fun actualizarGraficoBarras(reportes: List<com.example.reportes.data.model.Reporte>) {
        val tvTituloGraficoBarras = findViewById<TextView>(R.id.tvTituloGraficoBarras)
        tvTituloGraficoBarras.text = when (tipoValue) {
            "Todos" -> "Niveles de riesgo - Todos los reportes"
            else -> "Niveles de riesgo - $tipoValue"
        }

        var nivelBajo = 0
        var nivelMedio = 0
        var nivelAlto = 0

        for (reporte in reportes) {
            when {
                reporte.nivelRiesgo.equals("Bajo", ignoreCase = true) -> nivelBajo++
                reporte.nivelRiesgo.equals("Medio", ignoreCase = true) -> nivelMedio++
                reporte.nivelRiesgo.equals("Alto", ignoreCase = true) -> nivelAlto++
            }
        }

        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, nivelBajo.toFloat()))
        entries.add(BarEntry(1f, nivelMedio.toFloat()))
        entries.add(BarEntry(2f, nivelAlto.toFloat()))

        val dataSet = BarDataSet(entries, "Niveles de riesgo")
        dataSet.colors = listOf(
            Color.parseColor("#00FF00"),
            Color.parseColor("#FFC000"),
            Color.parseColor("#EF2121")
        )
        dataSet.valueTextSize = 14f
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (value == 0f) "" else value.toInt().toString()
            }
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.6f

        barChart.data = barData

        // --- AJUSTE DEL EJE Y IZQUIERDO ---
        val leftAxis = barChart.axisLeft
        leftAxis.axisMinimum = 0f

        // Calcular el máximo valor y ajustar al siguiente múltiplo de 5
        val maxValue = maxOf(nivelBajo, nivelMedio, nivelAlto)
        val axisMax = if (maxValue <= 5) 5 else ((maxValue + 4) / 5) * 5
        leftAxis.axisMaximum = axisMax.toFloat()

        // Etiquetas cada 1 si es <= 5, si no cada 5
        if (axisMax <= 5) {
            leftAxis.setLabelCount(axisMax + 1, true) // 0,1,2,3,4,5
            leftAxis.granularity = 1f
        } else {
            leftAxis.setLabelCount((axisMax / 5) + 1, true) // 0,5,10,15...
            leftAxis.granularity = 5f
        }

        barChart.invalidate()
    }

    /**
     * Actualiza el objeto PieChart con los nuevos datos
     */
    private fun actualizarPieChart(entries: List<PieEntry>) {
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            Color.parseColor("#FFFF00"), // Pendiente
            Color.parseColor("#2F5597"), // Asignado
            Color.parseColor("#00FF00"), // Cerrado
            Color.parseColor("#FFA500")  // Cerrado parcial (naranja)
        )
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 16f
        dataSet.setDrawValues(false)
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                return "${value.toInt()}%"
            }
        }

        // Forzar que las porciones con 0% tengan un mínimo para que se vean
        val minPorcion = 0.5f
        val valores = entries.map { it.value }
        val total = valores.sum()
        val ajustados = if (total == 0f) {
            listOf(minPorcion, minPorcion, minPorcion, minPorcion)
        } else {
            valores.map { if (it == 0f) minPorcion else it }
        }
        val entriesAjustados = listOf(
            PieEntry(ajustados[0], "Pendiente"),
            PieEntry(ajustados[1], "Asignado"),
            PieEntry(ajustados[2], "Cerrado"),
            PieEntry(ajustados[3], "Cerrado parcial")
        )
        val pieData = PieData(PieDataSet(entriesAjustados, "").apply {
            colors = dataSet.colors
            valueTextColor = dataSet.valueTextColor
            valueTextSize = dataSet.valueTextSize
            setDrawValues(false)
            valueFormatter = dataSet.valueFormatter
        })
        pieChart.data = pieData
        pieChart.invalidate()
    }

    // SECCIÓN: EXPORTACIÓN DE DATOS
    /**
     * Exporta los datos a un archivo Excel
     */
    private fun exportarDatosAExcel(datos: List<Pair<com.example.reportes.data.model.Reporte, com.example.reportes.data.model.ReporteCerrado?>>) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Informe Consolidado")

        // --- TÍTULO ---
        val titleRow = sheet.createRow(0)
        val titleCell = titleRow.createCell(0)
        titleCell.setCellValue("Informe Consolidado de Reportes")
        val titleStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply { fontHeightInPoints = 16; bold = true }
            setFont(font)
            alignment = HorizontalAlignment.CENTER
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }
        titleCell.cellStyle = titleStyle
        
        // Establecer altura de la fila del título (40 unidades en lugar de la altura predeterminada)
        titleRow.height = 400

        // --- ENCABEZADOS ---
        val headers = listOf(
            "id", "tipo", "nivelRiesgo", "descripcion", "imagenUrl_reporte", "lugar",
            "reportante", "estado", "responsable", "fechacreacion", "fechaAsignacion",
            "fechaLimite", "comentario_cierre", "imagenUrl_levantamiento", "motivo_rechazo",
            "estado_cierre", "fechalevantamiento"
        )
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, headers.size - 1))

        val headerRow = sheet.createRow(1)
        val headerStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply { bold = true }
            setFont(font)
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
        }
        headers.forEachIndexed { i, header ->
            val cell = headerRow.createCell(i)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }

        // --- DATOS ---
        datos.forEachIndexed { index, (reporte, reporteCerrado) ->
            val row = sheet.createRow(index + 2)

            row.createCell(0).setCellValue(reporte.id)
            row.createCell(1).setCellValue(reporte.tipo)
            row.createCell(2).setCellValue(reporte.nivelRiesgo)
            row.createCell(3).setCellValue(reporte.descripcion)
            row.createCell(4).setCellValue(reporte.imagenUrl)
            row.createCell(5).setCellValue(reporte.lugar)
            row.createCell(6).setCellValue(reporte.reportante)
            row.createCell(7).setCellValue(reporte.estado)
            row.createCell(8).setCellValue(reporte.responsable)
            row.createCell(9).setCellValue(reporte.fechacreacion)
            row.createCell(10).setCellValue(reporte.fechaAsignacion)
            row.createCell(11).setCellValue(reporte.fechaLimite)
            // Datos de la colección 'reportesCerrados'
            row.createCell(12).setCellValue(reporteCerrado?.comentario ?: "")
            row.createCell(13).setCellValue(reporteCerrado?.imagenUrl ?: "")
            row.createCell(14).setCellValue(reporteCerrado?.motivo ?: "")
            row.createCell(15).setCellValue(reporteCerrado?.estadoCierre ?: "")
            row.createCell(16).setCellValue(reporteCerrado?.fechalevantamiento ?: "")
        }

        // Ajuste manual de ancho de columnas
        for (col in headers.indices) {
            var maxLength = headers[col].length
            for (rowIdx in 2..(datos.size + 1)) {
                val row = sheet.getRow(rowIdx) ?: continue
                val cell = row.getCell(col)
                val value = cell?.toString() ?: ""
                
                // Limitar el tamaño máximo de las columnas con URLs
                val adjustedLength = when (col) {
                    4, 13 -> minOf(value.length, 40) // Columnas de URLs: limitar a 40 caracteres
                    else -> value.length
                }
                
                if (adjustedLength > maxLength) maxLength = adjustedLength
            }
            
            // Ajustar ancho específico para columnas de URLs
            val columnWidth = when (col) {
                4, 13 -> minOf((maxLength + 1) * 256, 40 * 256) // Reducido a 40 unidades máximo (era 50)
                else -> (maxLength + 2) * 256
            }
            sheet.setColumnWidth(col, columnWidth)
        }

        mostrarDialogoNombreArchivo(workbook)
    }

    /**
     * Muestra un diálogo para definir el nombre del archivo a exportar
     */
    private fun mostrarDialogoNombreArchivo(workbook: XSSFWorkbook) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_guardar_archivo, null)
        val editText = dialogLayout.findViewById<EditText>(R.id.etNombreArchivo)

        builder.setView(dialogLayout)
            .setCancelable(false) // <-- Solo se puede cerrar con botones
            .setPositiveButton("Guardar") { dialog, _ ->
                val nombreArchivo = editText.text.toString()
                if (nombreArchivo.isNotBlank()) {
                    guardarExcel(workbook, nombreArchivo)
                } else {
                    Toast.makeText(this, "Error al descargar: el nombre no puede estar vacío.", Toast.LENGTH_SHORT).show()
                }
                exportDialogAbierto = false
                findViewById<Button>(R.id.btnExportar).isEnabled = true
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                Toast.makeText(this, "Exportación cancelada.", Toast.LENGTH_SHORT).show()
                exportDialogAbierto = false
                findViewById<Button>(R.id.btnExportar).isEnabled = true
                dialog.cancel()
            }

        val dialog = builder.create()
        dialog.show()
    }

    /**
     * Guarda el archivo Excel en el almacenamiento
     */
    private fun guardarExcel(workbook: XSSFWorkbook, nombreArchivoBase: String) {
        val nombreArchivo = "$nombreArchivoBase.xlsx"
        val mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10 (API 29) y superior
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, nombreArchivo)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Reportes")
                }

                val resolver = contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                uri?.let {
                    resolver.openOutputStream(it).use { outputStream ->
                        workbook.write(outputStream)
                    }
                    Toast.makeText(this, "Archivo exportado en Descargas/Reportes", Toast.LENGTH_LONG).show()
                } ?: throw Exception("No se pudo crear el archivo en MediaStore.")

            } else {
                // Versiones anteriores a Android 10
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val reportesDir = File(downloadsDir, "Reportes")

                if (!reportesDir.exists()) {
                    if (!reportesDir.mkdirs()) {
                        throw Exception("No se pudo crear el directorio Reportes.")
                    }
                }

                val file = File(reportesDir, nombreArchivo)
                FileOutputStream(file).use { fos ->
                    workbook.write(fos)
                }
                Toast.makeText(this, "Archivo exportado en Descargas/Reportes: $nombreArchivo", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al exportar: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            try {
                workbook.close()
            } catch (e: Exception) {
                // Opcional: Loggear error al cerrar el workbook
            }
        }
    }

    /**
     * Función auxiliar para truncar URLs largas
     */
    private fun truncarUrl(url: String): String {
        if (url.length <= 40) return url
        
        // Buscar un punto o slash para cortar de forma más elegante
        val puntoCorte = url.lastIndexOf("/", 35)
        if (puntoCorte > 20) {
            return url.substring(0, puntoCorte) + "..."
        }
        
        // Si no hay un buen punto de corte, simplemente truncar
        return url.substring(0, 37) + "..."
    }

    // SECCIÓN: CLASES INTERNAS
    /**
     * Renderizador personalizado para el gráfico de torta
     */
    class CustomPieChartRenderer(
        chart: PieChart,
        animator: ChartAnimator,
        viewPortHandler: ViewPortHandler
    ) : PieChartRenderer(chart, animator, viewPortHandler) {

        // Estilos de pintura
        private val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        private val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 38f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Paints para cada estado cuando no hay datos
        private val placeholderPaints = listOf(
            Paint().apply { color = Color.YELLOW; style = Paint.Style.FILL; isAntiAlias = true },
            Paint().apply { color = Color.parseColor("#2946A4"); style = Paint.Style.FILL; isAntiAlias = true },
            Paint().apply { color = Color.parseColor("#00FF00"); style = Paint.Style.FILL; isAntiAlias = true },
            Paint().apply { color = Color.parseColor("#FFA500"); style = Paint.Style.FILL; isAntiAlias = true } // Naranja
        )

        override fun drawData(c: Canvas) {
            if (mChart.data != null && mChart.data.yValueSum == 0f) {
                // Si no hay datos, dibujamos manualmente el gráfico y los valores "0%"
                drawPlaceholder(c)
            } else {
                // Si hay datos, usa el comportamiento normal
                super.drawData(c)
            }
        }

        private fun drawPlaceholder(c: Canvas) {
            val circleBox = mChart.circleBox
            val sweepAngle = 90f // 360/4
            // Usamos la rotación del gráfico para que los arcos siempre empiecen en la misma posición visual
            var startAngle = mChart.rotationAngle - 90f

            // Dibuja las 4 secciones de color
            for (paint in placeholderPaints) {
                c.drawArc(circleBox, startAngle, sweepAngle, true, paint)
                startAngle += sweepAngle
            }
        }

        override fun drawValues(c: Canvas) {
            if (mChart.data != null && mChart.data.yValueSum == 0f) {
                // Si no hay datos, dibujamos las etiquetas "0%" manualmente
                drawPlaceholderValues(c)
            } else {
                // Si hay datos, usa el comportamiento normal para dibujar los valores
                drawActualValues(c)
            }
        }

        private fun drawPlaceholderValues(c: Canvas) {
            val sweepAngle = 90f
            var startAngle = mChart.rotationAngle - 90f
            val radius = 48f
            val r = mChart.radius * 0.7f
            val center = mChart.centerCircleBox

            for (i in 0 until 4) {
                val labelAngle = startAngle + sweepAngle / 2f
                val x = center.x + r * Math.cos(Math.toRadians(labelAngle.toDouble())).toFloat()
                val y = center.y + r * Math.sin(Math.toRadians(labelAngle.toDouble())).toFloat()

                // Dibuja fondo blanco redondeado
                val rect = RectF(x - radius, y - radius / 2, x + radius, y + radius / 2)
                c.drawRoundRect(rect, 20f, 20f, bgPaint)

                // Dibuja el texto "0%"
                val textY = y + textPaint.textSize / 3
                c.drawText("0%", x, textY, textPaint)

                startAngle += sweepAngle
            }
        }

        private fun drawActualValues(c: Canvas) {
            val dataSet = mChart.data.dataSet
            val radius = 48f

            for (i in 0 until dataSet.entryCount) {
                val entry = dataSet.getEntryForIndex(i)

                val angle = mChart.rotationAngle + mChart.drawAngles.take(i).sum() + mChart.drawAngles[i] / 2f
                val r = mChart.radius * 0.7f
                val center = mChart.centerCircleBox
                val x = center.x + r * Math.cos(Math.toRadians(angle.toDouble())).toFloat()
                val y = center.y + r * Math.sin(Math.toRadians(angle.toDouble())).toFloat()

                // Dibuja fondo blanco redondeado
                val rect = RectF(x - radius, y - radius / 2, x + radius, y + radius / 2)
                c.drawRoundRect(rect, 20f, 20f, bgPaint)

                // Dibuja el texto encima del fondo blanco
                val porcentaje = "${entry.value.toInt()}%"
                val textY = y + textPaint.textSize / 3
                c.drawText(porcentaje, x, textY, textPaint)
            }
        }
    }
}