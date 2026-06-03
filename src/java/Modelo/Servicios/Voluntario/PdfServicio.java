package Modelo.Servicios.Voluntario;

import Modelo.Config.Conexion;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.File;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

// Qué hace: Servicio encargado de compilar y generar el documento PDF del Plan de Emergencia Familiar.
// Por qué existe: Extrae toda la información registrada del plan y sus relaciones desde la base de datos y la exporta en formato de la Defensa Civil Colombiana.
// Qué problema resuelve: Consolida múltiples tablas de datos (miembros, mascotas, recursos, planes de acción) y gráficos físicos en un único reporte PDF con formato institucional oficial.
public class PdfServicio {

    // =========================================================================
    // PALETA DE COLORES INSTITUCIONAL - DEFENSA CIVIL COLOMBIANA
    // =========================================================================

    // Azul institucional principal de la Defensa Civil (encabezados y títulos)
    private static final java.awt.Color COLOR_AZUL_DC = new java.awt.Color(0, 51, 102);
    // Naranja institucional de emergencia (acentos y alertas)
    private static final java.awt.Color COLOR_NARANJA_DC = new java.awt.Color(230, 126, 34);
    // Gris claro para fondos alternos de filas (zebra)
    private static final java.awt.Color COLOR_GRIS_CLARO = new java.awt.Color(241, 245, 249);
    // Gris más suave para bordes de tabla
    private static final java.awt.Color COLOR_BORDE = new java.awt.Color(189, 195, 199);
    // Blanco y negro estándar
    private static final java.awt.Color COLOR_BLANCO = java.awt.Color.WHITE;
    private static final java.awt.Color COLOR_NEGRO = java.awt.Color.BLACK;
    // Azul claro para subtítulos de sección (fondo de la fila de cabecera de sección)
    private static final java.awt.Color COLOR_AZUL_CLARO = new java.awt.Color(214, 234, 248);

    // =========================================================================
    // FUENTES TIPOGRÁFICAS DEL DOCUMENTO
    // =========================================================================

    // Título principal del documento (Plan Familiar de Emergencia)
    private final Font fontTituloPrincipal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, COLOR_AZUL_DC);
    // Subtítulo institucional (Defensa Civil Colombiana)
    private final Font fontSubtitulo = FontFactory.getFont(FontFactory.HELVETICA, 10, COLOR_AZUL_DC);
    // Encabezado de sección numerada (1. IDENTIFICACIÓN, 2. INTEGRANTES, etc.)
    private final Font fontSeccion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COLOR_BLANCO);
    // Subsección o grupo interno (ej: "Datos de Vivienda")
    private final Font fontSubSeccion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COLOR_AZUL_DC);
    // Encabezado de columna en tabla de datos
    private final Font fontCabeceraTabla = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, COLOR_BLANCO);
    // Etiqueta de campo tipo formulario (campo izquierdo de par clave-valor)
    private final Font fontEtiqueta = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, COLOR_AZUL_DC);
    // Valor de campo tipo formulario (campo derecho)
    private final Font fontValor = FontFactory.getFont(FontFactory.HELVETICA, 7, COLOR_NEGRO);
    // Texto de cuerpo en celdas de tabla
    private final Font fontCuerpo = FontFactory.getFont(FontFactory.HELVETICA, 7, COLOR_NEGRO);
    // Texto pequeño para pie de página y metadata
    private final Font fontPie = FontFactory.getFont(FontFactory.HELVETICA, 6, java.awt.Color.GRAY);
    // Texto en negrilla para resaltar dentro de cuerpo
    private final Font fontCuerpoNegrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, COLOR_NEGRO);
    // Momento del plan de acción (Antes/Durante/Después)
    private final Font fontMomento = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, COLOR_BLANCO);

    // =========================================================================
    // MÉTODO PRINCIPAL: GENERACIÓN DEL PDF COMPLETO
    // =========================================================================

    // Qué hace: Orquesta la creación del PDF completo del plan de emergencia, conectándose a la BD para extraer todos los datos y escribiendo bytes al OutputStream.
    // Por qué existe: Es el punto de entrada principal invocado desde el PdfServlet para generar el documento.
    // Qué problema resuelve: Abre y cierra de forma segura el documento iText, agrega encabezados/pies institucionales y maneja excepciones SQL o de E/S.
    public void generarPlanEmergenciaPDF(int planId, String contextPath, OutputStream out) throws Exception {
        // Documento en formato Carta (Letter) con márgenes: superior 90pt para encabezado, inferior 50pt para pie
        Document document = new Document(PageSize.LETTER, 36, 36, 90, 50);
        PdfWriter writer = PdfWriter.getInstance(document, out);

        // Pie de página institucional con número de página
        HeaderFooter footer = new HeaderFooter(
            new Phrase("Defensa Civil Colombiana — Plan Familiar de Emergencia | Pág. ", fontPie), true
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setBorder(Rectangle.TOP);
        footer.setBorderColor(COLOR_BORDE);
        document.setFooter(footer);

        document.open();

        try (Connection con = Conexion.obtener()) {
            // ===== ENCABEZADO INSTITUCIONAL =====
            construirEncabezadoInstitucional(document, contextPath);

            // ===== SECCIÓN 1: Identificación Familiar =====
            agregarBannerSeccion(document, "1. IDENTIFICACIÓN FAMILIAR Y VIVIENDA");
            construirTablaIdentificacion(con, planId, document);

            // ===== SECCIÓN 2: Integrantes del Hogar =====
            agregarBannerSeccion(document, "2. INTEGRANTES DE LA FAMILIA");
            construirTablaIntegrantes(con, planId, document);

            // ===== SECCIÓN 3: Mascotas =====
            agregarBannerSeccion(document, "3. MASCOTAS Y ANIMALES DOMÉSTICOS");
            construirTablaMascotas(con, planId, document);

            // ===== SECCIÓN 4: Recursos Disponibles =====
            agregarBannerSeccion(document, "4. RECURSOS DISPONIBLES Y ENTIDADES DE APOYO");
            construirTablaRecursos(con, planId, document);

            // ===== SECCIÓN 5: Plan de Acción =====
            agregarBannerSeccion(document, "5. PLAN DE ACCIÓN FAMILIAR (ANTES, DURANTE Y DESPUÉS)");
            construirTablaPlanAccion(con, planId, document);

            // ===== SECCIÓN 6: Gráficos y Croquis =====
            agregarBannerSeccion(document, "6. GRÁFICOS, CROQUIS Y GEORREFERENCIACIÓN");
            inyectarGraficos(con, planId, contextPath, document);

        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    // =========================================================================
    // ENCABEZADO INSTITUCIONAL CON LOGO
    // =========================================================================

    // Qué hace: Dibuja el encabezado superior del documento con logo de la Defensa Civil, título oficial y subtítulo.
    // Por qué existe: Replica el formato de la cartilla oficial de la 5ta Edición.
    // Qué problema resuelve: Brinda identidad institucional al documento generado.
    private void construirEncabezadoInstitucional(Document document, String contextPath) throws Exception {
        // Tabla de 3 columnas: logo izquierdo | texto central | fecha derecha
        PdfPTable header = new PdfPTable(3);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1.5f, 5.0f, 1.5f});
        header.setSpacingAfter(10);

        // Columna 1: Logo institucional
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.BOX);
        logoCell.setBorderColor(COLOR_AZUL_DC);
        logoCell.setPadding(5);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        // Intentar cargar el logo desde la ruta del proyecto desplegado
        try {
            String logoPath = contextPath + File.separator + "assets" + File.separator + "logo.png";
            File logoFile = new File(logoPath);
            if (logoFile.exists()) {
                Image logo = Image.getInstance(logoPath);
                // Escalar el logo a un tamaño apropiado para el encabezado
                logo.scaleToFit(60f, 60f);
                logoCell.addElement(logo);
            } else {
                // Si no existe logo, mostrar texto alternativo
                logoCell.addElement(new Phrase("DC", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, COLOR_AZUL_DC)));
            }
        } catch (Exception e) {
            // Fallback si hay error al cargar la imagen
            logoCell.addElement(new Phrase("DC", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, COLOR_AZUL_DC)));
        }
        header.addCell(logoCell);

        // Columna 2: Texto institucional central
        PdfPCell textCell = new PdfPCell();
        textCell.setBorder(Rectangle.BOX);
        textCell.setBorderColor(COLOR_AZUL_DC);
        textCell.setPadding(8);
        textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        textCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        // Línea 1: nombre de la entidad
        Paragraph lineaEntidad = new Paragraph("DEFENSA CIVIL COLOMBIANA", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_AZUL_DC));
        lineaEntidad.setAlignment(Element.ALIGN_CENTER);
        textCell.addElement(lineaEntidad);

        // Línea 2: título del documento
        Paragraph lineaTitulo = new Paragraph("PLAN FAMILIAR DE EMERGENCIA", fontTituloPrincipal);
        lineaTitulo.setAlignment(Element.ALIGN_CENTER);
        textCell.addElement(lineaTitulo);

        // Línea 3: edición de referencia
        Paragraph lineaEdicion = new Paragraph("5ta Edición — 2025", fontSubtitulo);
        lineaEdicion.setAlignment(Element.ALIGN_CENTER);
        textCell.addElement(lineaEdicion);

        header.addCell(textCell);

        // Columna 3: Fecha de generación
        PdfPCell fechaCell = new PdfPCell();
        fechaCell.setBorder(Rectangle.BOX);
        fechaCell.setBorderColor(COLOR_AZUL_DC);
        fechaCell.setPadding(5);
        fechaCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        fechaCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        Paragraph fechaLabel = new Paragraph("Fecha:", fontEtiqueta);
        fechaLabel.setAlignment(Element.ALIGN_CENTER);
        fechaCell.addElement(fechaLabel);

        Paragraph fechaValor = new Paragraph(sdf.format(new Date()), fontValor);
        fechaValor.setAlignment(Element.ALIGN_CENTER);
        fechaCell.addElement(fechaValor);

        SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm");
        Paragraph horaLabel = new Paragraph("Hora:", fontEtiqueta);
        horaLabel.setAlignment(Element.ALIGN_CENTER);
        fechaCell.addElement(horaLabel);

        Paragraph horaValor = new Paragraph(sdfHora.format(new Date()), fontValor);
        horaValor.setAlignment(Element.ALIGN_CENTER);
        fechaCell.addElement(horaValor);

        header.addCell(fechaCell);

        document.add(header);
    }

    // =========================================================================
    // BANNER DE SECCIÓN NUMERADA
    // =========================================================================

    // Qué hace: Agrega una barra de sección con fondo azul oscuro institucional y texto blanco en mayúsculas.
    // Por qué existe: Replica el estilo de la cartilla oficial donde cada sección tiene un banner de color.
    // Qué problema resuelve: Separa visualmente las secciones del plan y da identidad al documento.
    private void agregarBannerSeccion(Document document, String titulo) throws DocumentException {
        // Tabla de una sola celda que actúa como banner con fondo azul DC
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        banner.setSpacingBefore(10);
        banner.setSpacingAfter(4);

        PdfPCell cell = new PdfPCell(new Phrase(titulo, fontSeccion));
        cell.setBackgroundColor(COLOR_AZUL_DC);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(COLOR_AZUL_DC);
        banner.addCell(cell);

        document.add(banner);
    }

    // =========================================================================
    // SECCIÓN 1: IDENTIFICACIÓN FAMILIAR Y VIVIENDA
    // =========================================================================

    // Qué hace: Consulta y dibuja una tabla tipo formulario con los datos de ubicación, familia y vivienda.
    // Por qué existe: Muestra la información censal y de localización de la familia en formato de grilla.
    // Qué problema resuelve: Maneja nulos y construye una tabla de 4 columnas (Clave-Valor x 2) con estilo de formulario.
    private void construirTablaIdentificacion(Connection con, int planId, Document document) throws Exception {
        // Consulta que reúne los datos de identificación familiar con sus catálogos asociados
        String sql = "SELECT ifa.nombre_familia, ifa.apellidos_familia, ifa.direccion, ifa.barrio_comuna_localidad, "
                   + "ifa.telefono_fijo, ifa.latitud, ifa.longitud, ifa.consentimiento_datos, "
                   + "tz.nombre AS zona_nombre, cv.nombre AS calidad_vivienda_nombre, s.nombre AS sector_nombre, "
                   + "tf.nombre AS tipo_familia_nombre "
                   + "FROM identificacion_familiar ifa "
                   + "LEFT JOIN planes_familiares pf ON ifa.plan_id = pf.id "
                   + "LEFT JOIN tipos_familia tf ON pf.tipo_familia_id = tf.id "
                   + "LEFT JOIN tipos_zona tz ON ifa.tipo_zona_id = tz.id "
                   + "LEFT JOIN calidad_vivienda cv ON ifa.calidad_vivienda_id = cv.id "
                   + "LEFT JOIN sectores s ON ifa.sector_id = s.id "
                   + "WHERE ifa.plan_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                // Tabla tipo formulario: 4 columnas (etiqueta | valor | etiqueta | valor)
                PdfPTable table = new PdfPTable(4);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{1.8f, 2.2f, 1.8f, 2.2f});

                if (rs.next()) {
                    // Fila 1: Nombre de familia y Apellidos
                    addCampoFormulario(table, "Nombre Familia:", rs.getString("nombre_familia"));
                    addCampoFormulario(table, "Apellidos:", rs.getString("apellidos_familia"));
                    // Fila 2: Tipo de familia y Calidad de vivienda
                    addCampoFormulario(table, "Tipo de Familia:", rs.getString("tipo_familia_nombre"));
                    addCampoFormulario(table, "Calidad Vivienda:", rs.getString("calidad_vivienda_nombre"));
                    // Fila 3: Dirección y Barrio/Comuna
                    addCampoFormulario(table, "Dirección:", rs.getString("direccion"));
                    addCampoFormulario(table, "Barrio / Comuna:", rs.getString("barrio_comuna_localidad"));
                    // Fila 4: Teléfono y Zona
                    addCampoFormulario(table, "Teléfono Fijo:", rs.getString("telefono_fijo"));
                    addCampoFormulario(table, "Zona:", rs.getString("zona_nombre"));
                    // Fila 5: Sector y Consentimiento
                    addCampoFormulario(table, "Sector:", rs.getString("sector_nombre"));
                    boolean consentimiento = rs.getBoolean("consentimiento_datos");
                    addCampoFormulario(table, "Consentimiento Datos:", consentimiento ? "Sí" : "No");
                    // Fila 6: Coordenadas geográficas
                    java.math.BigDecimal lat = rs.getBigDecimal("latitud");
                    java.math.BigDecimal lon = rs.getBigDecimal("longitud");
                    String latStr = lat != null ? lat.toString() : "No registrada";
                    String lonStr = lon != null ? lon.toString() : "No registrada";
                    addCampoFormulario(table, "Latitud:", latStr);
                    addCampoFormulario(table, "Longitud:", lonStr);
                } else {
                    // Si no hay datos de identificación registrados
                    PdfPCell cell = new PdfPCell(new Phrase("Identificación familiar no registrada.", fontCuerpo));
                    cell.setColspan(4);
                    cell.setPadding(8);
                    cell.setBorderColor(COLOR_BORDE);
                    table.addCell(cell);
                }
                document.add(table);
            }
        }
    }

    // =========================================================================
    // SECCIÓN 2: INTEGRANTES DE LA FAMILIA
    // =========================================================================

    // Qué hace: Consulta la lista de integrantes familiares con sus datos médicos y documentales.
    // Por qué existe: Modela la tabla de miembros con parentesco, documento, edad, sangre, salud.
    // Qué problema resuelve: Ejecuta consultas cruzadas para integrar afecciones médicas y medicamentos en una sola tabla.
    private void construirTablaIntegrantes(Connection con, int planId, Document document) throws Exception {
        // Consulta de integrantes con sus relaciones de catálogo
        String sql = "SELECT i.id, i.nombre, i.apellido, i.numero_documento, i.fecha_nacimiento, i.celular, "
                   + "i.eps, i.es_jefe_hogar, "
                   + "td.sigla AS tipo_documento, p.nombre AS parentesco, g.nombre AS grupo_sanguineo, "
                   + "n.nombre AS nacionalidad, ge.nombre AS genero "
                   + "FROM integrantes i "
                   + "LEFT JOIN tipo_documentos td ON i.tipo_documento_id = td.id "
                   + "LEFT JOIN parentescos p ON i.parentesco_id = p.id "
                   + "LEFT JOIN grupos_sanguineos g ON i.grupo_sanguineo_id = g.id "
                   + "LEFT JOIN nacionalidades n ON i.nacionalidad_id = n.id "
                   + "LEFT JOIN generos ge ON i.genero_id = ge.id "
                   + "WHERE i.plan_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                // Tabla de 8 columnas para mostrar todos los datos de cada integrante
                PdfPTable table = new PdfPTable(8);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{1.8f, 1.0f, 1.4f, 0.6f, 0.8f, 0.9f, 0.8f, 2.2f});

                // Encabezados de columna con fondo azul institucional
                addCeldaCabecera(table, "Nombre Completo");
                addCeldaCabecera(table, "Parentesco");
                addCeldaCabecera(table, "Documento");
                addCeldaCabecera(table, "Edad");
                addCeldaCabecera(table, "Sangre");
                addCeldaCabecera(table, "Celular");
                addCeldaCabecera(table, "EPS");
                addCeldaCabecera(table, "Afecciones / Medicamentos");

                boolean tieneRegistros = false;
                int fila = 0;
                while (rs.next()) {
                    tieneRegistros = true;
                    int memberId = rs.getInt("id");
                    String fullName = rs.getString("nombre") + " " + rs.getString("apellido");
                    // Marcar con asterisco si es jefe de hogar
                    boolean esJefe = rs.getBoolean("es_jefe_hogar");
                    if (esJefe) fullName = "★ " + fullName;

                    String kinship = rs.getString("parentesco");
                    String docType = rs.getString("tipo_documento");
                    String docNum = rs.getString("numero_documento");
                    String docStr = (docType != null ? docType : "") + " " + (docNum != null ? docNum : "No reg.");

                    // Calcular edad a partir de la fecha de nacimiento
                    java.sql.Date birthDate = rs.getDate("fecha_nacimiento");
                    int edad = calcularEdad(birthDate);
                    String edadStr = birthDate != null ? String.valueOf(edad) : "N/R";

                    String blood = rs.getString("grupo_sanguineo");
                    String phone = rs.getString("celular");
                    String eps = rs.getString("eps");

                    // Consultar afecciones médicas y medicamentos del integrante
                    String afeccionesStr = obtenerAfeccionesConMedicamentos(con, memberId);

                    // Color de fondo alterno (zebra) para mejorar legibilidad
                    java.awt.Color bgColor = (fila % 2 == 0) ? COLOR_BLANCO : COLOR_GRIS_CLARO;

                    addCeldaCuerpo(table, fullName, bgColor);
                    addCeldaCuerpo(table, kinship != null ? kinship : "Por definir", bgColor);
                    addCeldaCuerpo(table, docStr, bgColor);
                    addCeldaCuerpo(table, edadStr, bgColor);
                    addCeldaCuerpo(table, blood != null ? blood : "N/R", bgColor);
                    addCeldaCuerpo(table, phone != null && !phone.isEmpty() ? phone : "N/R", bgColor);
                    addCeldaCuerpo(table, eps != null && !eps.isEmpty() ? eps : "N/R", bgColor);
                    addCeldaCuerpo(table, afeccionesStr, bgColor);
                    fila++;
                }

                if (!tieneRegistros) {
                    PdfPCell cell = new PdfPCell(new Phrase("No hay integrantes familiares registrados.", fontCuerpo));
                    cell.setColspan(8);
                    cell.setPadding(8);
                    cell.setBorderColor(COLOR_BORDE);
                    table.addCell(cell);
                }
                document.add(table);
            }
        }
    }

    // =========================================================================
    // SECCIÓN 3: MASCOTAS Y ANIMALES DOMÉSTICOS
    // =========================================================================

    // Qué hace: Consulta las mascotas del plan y muestra especie, raza, género, edad y vacunas.
    // Por qué existe: Los animales de compañía también deben contemplarse en la evacuación.
    // Qué problema resuelve: Concatena las vacunas de cada mascota en una sola celda.
    private void construirTablaMascotas(Connection con, int planId, Document document) throws Exception {
        // Consulta de mascotas con su especie asociada
        String sql = "SELECT m.id, m.nombre, m.raza, m.genero_animal, m.fecha_nacimiento, em.nombre AS especie_nombre "
                   + "FROM mascotas m "
                   + "LEFT JOIN especies_mascota em ON m.especie_id = em.id "
                   + "WHERE m.plan_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                PdfPTable table = new PdfPTable(6);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{1.5f, 1.2f, 1.5f, 0.8f, 1.0f, 2.5f});

                addCeldaCabecera(table, "Nombre");
                addCeldaCabecera(table, "Especie");
                addCeldaCabecera(table, "Raza");
                addCeldaCabecera(table, "Género");
                addCeldaCabecera(table, "Edad");
                addCeldaCabecera(table, "Vacunas Registradas");

                boolean tieneRegistros = false;
                int fila = 0;
                while (rs.next()) {
                    tieneRegistros = true;
                    int petId = rs.getInt("id");
                    String name = rs.getString("nombre");
                    String species = rs.getString("especie_nombre");
                    String breed = rs.getString("raza");
                    String gender = rs.getString("genero_animal");

                    // Calcular edad de la mascota
                    java.sql.Date birthDate = rs.getDate("fecha_nacimiento");
                    String edadMascota = calcularEdadMascota(birthDate);

                    // Consultar vacunas aplicadas con fechas
                    String vacunasStr = obtenerVacunasTexto(con, petId);

                    java.awt.Color bgColor = (fila % 2 == 0) ? COLOR_BLANCO : COLOR_GRIS_CLARO;

                    addCeldaCuerpo(table, name, bgColor);
                    addCeldaCuerpo(table, species != null ? species : "Otro", bgColor);
                    addCeldaCuerpo(table, breed != null && !breed.isEmpty() ? breed : "Sin raza", bgColor);
                    addCeldaCuerpo(table, gender != null ? gender : "N/R", bgColor);
                    addCeldaCuerpo(table, edadMascota, bgColor);
                    addCeldaCuerpo(table, vacunasStr, bgColor);
                    fila++;
                }

                if (!tieneRegistros) {
                    PdfPCell cell = new PdfPCell(new Phrase("No se registraron mascotas en la vivienda.", fontCuerpo));
                    cell.setColspan(6);
                    cell.setPadding(8);
                    cell.setBorderColor(COLOR_BORDE);
                    table.addCell(cell);
                }
                document.add(table);
            }
        }
    }

    // =========================================================================
    // SECCIÓN 4: RECURSOS DISPONIBLES Y ENTIDADES DE APOYO
    // =========================================================================

    // Qué hace: Lista los recursos comunitarios e instituciones de emergencia cercanos a la vivienda.
    // Por qué existe: Es parte fundamental del plan saber a dónde acudir en caso de emergencia.
    // Qué problema resuelve: Presenta los recursos con distancia, teléfono y descripción en formato tabular.
    private void construirTablaRecursos(Connection con, int planId, Document document) throws Exception {
        // Consulta de recursos disponibles con su tipo asociado
        String sql = "SELECT r.nombre_lugar, r.distancia_metros, r.telefono, r.descripcion, tr.nombre AS tipo_recurso "
                   + "FROM recursos_disponibles r "
                   + "LEFT JOIN tipos_recurso tr ON r.tipo_recurso_id = tr.id "
                   + "WHERE r.plan_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                PdfPTable table = new PdfPTable(5);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{2.2f, 1.5f, 1.0f, 1.2f, 2.1f});

                addCeldaCabecera(table, "Nombre del Lugar / Recurso");
                addCeldaCabecera(table, "Tipo de Recurso");
                addCeldaCabecera(table, "Distancia");
                addCeldaCabecera(table, "Teléfono");
                addCeldaCabecera(table, "Descripción");

                boolean tieneRegistros = false;
                int fila = 0;
                while (rs.next()) {
                    tieneRegistros = true;
                    String place = rs.getString("nombre_lugar");
                    String type = rs.getString("tipo_recurso");
                    int dist = rs.getInt("distancia_metros");
                    String phone = rs.getString("telefono");
                    String desc = rs.getString("descripcion");

                    java.awt.Color bgColor = (fila % 2 == 0) ? COLOR_BLANCO : COLOR_GRIS_CLARO;

                    addCeldaCuerpo(table, place, bgColor);
                    addCeldaCuerpo(table, type != null ? type : "No especificado", bgColor);
                    addCeldaCuerpo(table, dist + " m", bgColor);
                    addCeldaCuerpo(table, phone != null && !phone.isEmpty() ? phone : "N/R", bgColor);
                    addCeldaCuerpo(table, desc != null && !desc.isEmpty() ? desc : "Sin descripción", bgColor);
                    fila++;
                }

                if (!tieneRegistros) {
                    PdfPCell cell = new PdfPCell(new Phrase("No hay recursos comunitarios o de apoyo registrados.", fontCuerpo));
                    cell.setColspan(5);
                    cell.setPadding(8);
                    cell.setBorderColor(COLOR_BORDE);
                    table.addCell(cell);
                }
                document.add(table);
            }
        }
    }

    // =========================================================================
    // SECCIÓN 5: PLAN DE ACCIÓN FAMILIAR
    // =========================================================================

    // Qué hace: Muestra la amenaza principal, el coordinador y la tabla de acciones por momento (Antes/Durante/Después).
    // Por qué existe: Es la columna vertebral del plan de emergencia mostrando qué hacer en cada fase.
    // Qué problema resuelve: Agrupa la metadata y las tareas ordenadamente con colores por momento.
    private void construirTablaPlanAccion(Connection con, int planId, Document document) throws Exception {
        // 1. Obtener metadata general del plan de acción (amenaza y coordinador)
        String sqlMeta = "SELECT pa.coordinador_id, i.nombre, i.apellido, am.nombre AS amenaza_nombre "
                       + "FROM plan_accion pa "
                       + "LEFT JOIN integrantes i ON pa.coordinador_id = i.id "
                       + "LEFT JOIN factores_riesgo fr ON pa.riesgo_id = fr.id "
                       + "LEFT JOIN amenazas am ON fr.amenaza_id = am.id "
                       + "WHERE pa.plan_id = ? LIMIT 1";

        String coordinadorGeneral = "Por definir";
        String amenazaGeneral = "Por definir";

        try (PreparedStatement psMeta = con.prepareStatement(sqlMeta)) {
            psMeta.setInt(1, planId);
            try (ResultSet rsMeta = psMeta.executeQuery()) {
                if (rsMeta.next()) {
                    String coordNombre = rsMeta.getString("nombre");
                    String coordApellido = rsMeta.getString("apellido");
                    if (coordNombre != null && coordApellido != null) {
                        coordinadorGeneral = coordNombre + " " + coordApellido;
                    }
                    String amenaza = rsMeta.getString("amenaza_nombre");
                    if (amenaza != null) {
                        amenazaGeneral = amenaza;
                    }
                }
            }
        }

        // Tabla de metadata superior tipo formulario (2 campos clave-valor)
        PdfPTable metaTable = new PdfPTable(4);
        metaTable.setWidthPercentage(100);
        metaTable.setSpacingAfter(4);
        metaTable.setWidths(new float[]{2.0f, 3.0f, 2.0f, 3.0f});

        addCampoFormulario(metaTable, "Amenaza / Riesgo:", amenazaGeneral);
        addCampoFormulario(metaTable, "Coordinador Familiar:", coordinadorGeneral);

        document.add(metaTable);

        // 2. Tabla de acciones ordenadas por momento
        String sqlTasks = "SELECT pa.momento, pa.descripcion_tarea, i.nombre, i.apellido "
                        + "FROM plan_accion pa "
                        + "LEFT JOIN integrantes i ON pa.coordinador_id = i.id "
                        + "WHERE pa.plan_id = ? "
                        + "ORDER BY FIELD(pa.momento, 'antes', 'durante', 'despues'), pa.id";

        try (PreparedStatement ps = con.prepareStatement(sqlTasks)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                PdfPTable table = new PdfPTable(3);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{1.3f, 4.5f, 2.2f});

                addCeldaCabecera(table, "Momento");
                addCeldaCabecera(table, "Acción o Tarea Específica");
                addCeldaCabecera(table, "Responsable");

                boolean tieneRegistros = false;
                while (rs.next()) {
                    tieneRegistros = true;
                    String momentoRaw = rs.getString("momento");
                    String momentoStr = momentoRaw != null ? momentoRaw.substring(0, 1).toUpperCase() + momentoRaw.substring(1) : "Antes";
                    String desc = rs.getString("descripcion_tarea");

                    String respNombre = rs.getString("nombre");
                    String respApellido = rs.getString("apellido");
                    String responsable = (respNombre != null && respApellido != null) ? respNombre + " " + respApellido : "Sin asignar";

                    // Color de fondo según el momento para diferenciar visualmente cada fase
                    java.awt.Color bgMomento = obtenerColorMomento(momentoRaw);

                    // Celda del momento con fondo de color
                    PdfPCell cellMomento = new PdfPCell(new Phrase(momentoStr, fontMomento));
                    cellMomento.setBackgroundColor(bgMomento);
                    cellMomento.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cellMomento.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    cellMomento.setPadding(5);
                    cellMomento.setBorderColor(COLOR_BORDE);
                    table.addCell(cellMomento);

                    addCeldaCuerpo(table, desc, COLOR_BLANCO);
                    addCeldaCuerpo(table, responsable, COLOR_BLANCO);
                }

                if (!tieneRegistros) {
                    PdfPCell cell = new PdfPCell(new Phrase("No hay micro-acciones configuradas en este plan.", fontCuerpo));
                    cell.setColspan(3);
                    cell.setPadding(8);
                    cell.setBorderColor(COLOR_BORDE);
                    table.addCell(cell);
                }
                document.add(table);
            }
        }
    }

    // =========================================================================
    // SECCIÓN 6: GRÁFICOS, CROQUIS Y GEORREFERENCIACIÓN
    // =========================================================================

    // Qué hace: Consulta las imágenes registradas (vivienda, entorno, mapa) y las inserta en el PDF.
    // Por qué existe: Integra los croquis de vivienda, entorno y mapa del plan en el reporte final.
    // Qué problema resuelve: Localiza físicamente en disco los archivos y los escala para que quepan en la página.
    private void inyectarGraficos(Connection con, int planId, String contextPath, Document document) throws Exception {
        String sql = "SELECT ruta_archivo, tipo_grafico, descripcion FROM imagenes WHERE plan_id = ? ORDER BY tipo_grafico, id";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                boolean tieneGraficos = false;

                while (rs.next()) {
                    tieneGraficos = true;
                    String relPath = rs.getString("ruta_archivo");
                    String tipo = rs.getString("tipo_grafico");
                    String desc = rs.getString("descripcion");

                    // Mapear nombre visual del tipo de gráfico según la cartilla oficial
                    String tipoLabel = "CROQUIS DE LA VIVIENDA";
                    if ("entorno".equalsIgnoreCase(tipo)) {
                        tipoLabel = "CROQUIS DEL ENTORNO FAMILIAR";
                    } else if ("mapa".equalsIgnoreCase(tipo)) {
                        tipoLabel = "MAPA DE GEORREFERENCIACIÓN";
                    }

                    // Resolver ruta absoluta del archivo en el servidor
                    String systemPath = relPath.replace("/", File.separator);
                    String absolutePath = contextPath + File.separator + systemPath;

                    File imgFile = new File(absolutePath);
                    if (imgFile.exists() && imgFile.isFile()) {
                        // Nueva página para que cada gráfico se presente limpiamente
                        document.newPage();

                        // Banner de subsección para el tipo de gráfico
                        PdfPTable subBanner = new PdfPTable(1);
                        subBanner.setWidthPercentage(100);
                        subBanner.setSpacingAfter(8);

                        PdfPCell bannerCell = new PdfPCell(new Phrase(tipoLabel, fontSeccion));
                        bannerCell.setBackgroundColor(COLOR_NARANJA_DC);
                        bannerCell.setPadding(5);
                        bannerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        bannerCell.setBorderColor(COLOR_NARANJA_DC);
                        subBanner.addCell(bannerCell);
                        document.add(subBanner);

                        // Insertar imagen escalada al tamaño de la página
                        Image image = Image.getInstance(absolutePath);
                        image.setAlignment(Element.ALIGN_CENTER);
                        // Escalar para que quepa en la hoja con márgenes (ancho máx ~480pt, alto máx ~400pt)
                        image.scaleToFit(480f, 400f);
                        image.setSpacingAfter(8);
                        document.add(image);

                        // Descripción del gráfico si existe
                        if (desc != null && !desc.trim().isEmpty()) {
                            Paragraph descP = new Paragraph("Descripción: " + desc, fontCuerpo);
                            descP.setAlignment(Element.ALIGN_CENTER);
                            descP.setSpacingAfter(10);
                            document.add(descP);
                        }
                    }
                }

                if (!tieneGraficos) {
                    Paragraph noImg = new Paragraph(
                        "No se han adjuntado croquis o mapas de georreferenciación en este plan familiar.",
                        fontCuerpo
                    );
                    noImg.setSpacingAfter(10);
                    document.add(noImg);
                }
            }
        }
    }

    // =========================================================================
    // HELPERS: CONSULTAS SECUNDARIAS A BASE DE DATOS
    // =========================================================================

    // Qué hace: Retorna las afecciones médicas de un integrante con sus medicamentos, concatenadas en texto.
    // Por qué existe: Enriquece la tabla de integrantes con información completa de salud.
    // Qué problema resuelve: Consulta dos tablas relacionadas (afecciones + medicamentos) y las formatea en una sola cadena.
    private String obtenerAfeccionesConMedicamentos(Connection con, int memberId) throws SQLException {
        // Primero consultar las afecciones del integrante
        String sqlAfecciones = "SELECT a.id, a.tipo, a.nombre_afeccion FROM afecciones WHERE integrante_id = ?";
        StringBuilder sb = new StringBuilder();

        try (PreparedStatement ps = con.prepareStatement(sqlAfecciones)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (sb.length() > 0) sb.append("; ");
                    int afeccionId = rs.getInt("id");
                    String tipo = rs.getString("tipo");
                    // Capitalizar el tipo de afección
                    String tipoStr = tipo != null ? tipo.substring(0, 1).toUpperCase() + tipo.substring(1) : "Enfermedad";
                    sb.append(tipoStr).append(": ").append(rs.getString("nombre_afeccion"));

                    // Consultar medicamentos asociados a esta afección
                    String sqlMeds = "SELECT nombre_droga, dosis_diaria FROM medicamentos WHERE afeccion_id = ?";
                    try (PreparedStatement psMeds = con.prepareStatement(sqlMeds)) {
                        psMeds.setInt(1, afeccionId);
                        try (ResultSet rsMeds = psMeds.executeQuery()) {
                            StringBuilder medsSb = new StringBuilder();
                            while (rsMeds.next()) {
                                if (medsSb.length() > 0) medsSb.append(", ");
                                medsSb.append(rsMeds.getString("nombre_droga"));
                            }
                            // Agregar medicamentos entre paréntesis si existen
                            if (medsSb.length() > 0) {
                                sb.append(" [").append(medsSb).append("]");
                            }
                        }
                    }
                }
            }
        }
        return sb.length() > 0 ? sb.toString() : "Ninguna";
    }

    // Qué hace: Retorna la lista de vacunas aplicadas a una mascota con fechas, concatenada en texto.
    // Por qué existe: Muestra el historial de vacunación en una sola celda de la tabla de mascotas.
    // Qué problema resuelve: Formatea múltiples registros de vacunas en una cadena legible.
    private String obtenerVacunasTexto(Connection con, int petId) throws SQLException {
        String sql = "SELECT nombre_vacuna, fecha_aplicacion FROM vacunas WHERE mascota_id = ?";
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, petId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (sb.length() > 0) sb.append("; ");
                    sb.append(rs.getString("nombre_vacuna"));
                    java.sql.Date fecha = rs.getDate("fecha_aplicacion");
                    if (fecha != null) {
                        sb.append(" (").append(sdf.format(fecha)).append(")");
                    }
                }
            }
        }
        return sb.length() > 0 ? sb.toString() : "Ninguna";
    }

    // Qué hace: Calcula la edad actual en años a partir de una fecha de nacimiento.
    // Por qué existe: Convierte fechas de nacimiento a edad legible para las tablas del PDF.
    // Qué problema resuelve: Evita mostrar fechas crudas y calcula la edad de forma precisa.
    private int calcularEdad(java.sql.Date birthDate) {
        if (birthDate == null) return 0;
        java.util.Calendar birth = java.util.Calendar.getInstance();
        birth.setTime(birthDate);
        java.util.Calendar today = java.util.Calendar.getInstance();
        int age = today.get(java.util.Calendar.YEAR) - birth.get(java.util.Calendar.YEAR);
        // Ajuste si aún no ha cumplido años en el año actual
        if (today.get(java.util.Calendar.DAY_OF_YEAR) < birth.get(java.util.Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age;
    }

    // Qué hace: Calcula la edad de una mascota mostrando años y meses.
    // Por qué existe: Las mascotas jóvenes pueden tener menos de 1 año y necesitan mostrar meses.
    // Qué problema resuelve: Formatea la edad de manera más descriptiva que solo años enteros.
    private String calcularEdadMascota(java.sql.Date birthDate) {
        if (birthDate == null) return "N/R";
        java.util.Calendar birth = java.util.Calendar.getInstance();
        birth.setTime(birthDate);
        java.util.Calendar today = java.util.Calendar.getInstance();

        int years = today.get(java.util.Calendar.YEAR) - birth.get(java.util.Calendar.YEAR);
        int months = today.get(java.util.Calendar.MONTH) - birth.get(java.util.Calendar.MONTH);

        if (months < 0) {
            years--;
            months += 12;
        }

        if (years > 0) {
            return years + " año" + (years > 1 ? "s" : "");
        } else {
            return months + " mes" + (months != 1 ? "es" : "");
        }
    }

    // Qué hace: Devuelve un color de fondo según la fase del plan de acción.
    // Por qué existe: Diferencia visualmente cada momento (Antes=verde, Durante=naranja, Después=azul).
    // Qué problema resuelve: Mejora la legibilidad del plan de acción en el PDF.
    private java.awt.Color obtenerColorMomento(String momento) {
        if (momento == null) return COLOR_AZUL_DC;
        switch (momento.toLowerCase()) {
            // Verde oscuro para acciones preventivas (Antes)
            case "antes":
                return new java.awt.Color(39, 174, 96);
            // Naranja de alerta para acciones durante la emergencia
            case "durante":
                return COLOR_NARANJA_DC;
            // Azul institucional para acciones de recuperación (Después)
            case "despues":
                return new java.awt.Color(52, 152, 219);
            default:
                return COLOR_AZUL_DC;
        }
    }

    // =========================================================================
    // HELPERS: CONSTRUCCIÓN DE CELDAS CON ESTILO INSTITUCIONAL
    // =========================================================================

    // Qué hace: Crea una celda de cabecera de tabla con fondo azul DC y texto blanco centrado.
    // Por qué existe: Estandariza el estilo de encabezados de todas las tablas del PDF.
    // Qué problema resuelve: Garantiza consistencia visual en todas las secciones.
    private void addCeldaCabecera(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, fontCabeceraTabla));
        cell.setBackgroundColor(COLOR_AZUL_DC);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        cell.setBorderColor(COLOR_BORDE);
        table.addCell(cell);
    }

    // Qué hace: Crea una celda de cuerpo de tabla con fondo personalizable y texto estándar.
    // Por qué existe: Permite aplicar estilo zebra (filas alternadas) de forma consistente.
    // Qué problema resuelve: Evita código repetido para cada celda de dato en las tablas.
    private void addCeldaCuerpo(PdfPTable table, String text, java.awt.Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", fontCuerpo));
        cell.setBackgroundColor(bgColor);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        cell.setBorderColor(COLOR_BORDE);
        table.addCell(cell);
    }

    // Qué hace: Agrega un par clave-valor (etiqueta + valor) tipo formulario a la tabla.
    // Por qué existe: Replica el estilo de la cartilla oficial donde los datos se muestran en grilla de formulario.
    // Qué problema resuelve: Dibuja dos celdas adyacentes con estilos distintos (negrita azul para la etiqueta, normal para el valor).
    private void addCampoFormulario(PdfPTable table, String key, String value) {
        // Celda de etiqueta con fondo gris claro
        PdfPCell cellKey = new PdfPCell(new Phrase(key, fontEtiqueta));
        cellKey.setBackgroundColor(COLOR_AZUL_CLARO);
        cellKey.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cellKey.setPadding(5);
        cellKey.setBorderColor(COLOR_BORDE);
        table.addCell(cellKey);

        // Celda de valor con fondo blanco
        PdfPCell cellVal = new PdfPCell(new Phrase(value != null && !value.isEmpty() ? value : "No registrado", fontValor));
        cellVal.setBackgroundColor(COLOR_BLANCO);
        cellVal.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cellVal.setPadding(5);
        cellVal.setBorderColor(COLOR_BORDE);
        table.addCell(cellVal);
    }
}
