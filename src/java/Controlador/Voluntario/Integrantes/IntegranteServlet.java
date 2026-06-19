package Controlador.Voluntario.Integrantes;

import Modelo.DTO.IntegranteDTO;
import Modelo.Servicios.Voluntario.IntegranteServicio;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import org.json.JSONObject;

/**
 * Qué hace: Servlet encargado de mapear las peticiones HTTP CRUD (GET, POST, PUT, DELETE) sobre la entidad integrantes (miembros de la familia).
 * Por qué existe: Actúa como el controlador de entrada para gestionar el flujo de datos de los integrantes de un plan de emergencia.
 * Qué pasaría si no estuviera: Los voluntarios no tendrían un enrutador para ingresar, ver, actualizar o dar de baja a los miembros de los núcleos familiares censados.
 */
@WebServlet("/api/members/*")
public class IntegranteServlet extends HttpServlet {

    // Qué hace: Instancia el servicio de lógica de negocios para el control de integrantes de la vivienda.
    // Por qué existe: Separa el enrutamiento de red de la lógica JDBC y de negocio de censados.
    // Qué pasaría si no estuviera: Deberíamos programar accesos de base de datos directos e inserciones complejas dentro de este servlet de presentación.
    // Flujo: De aquí pasamos a IntegranteServicio.
    private final IntegranteServicio servicio = new IntegranteServicio();

    // Qué hace: Atiende peticiones GET para listar integrantes de un plan (bajo /familyPlan/{planId}) o consultar los detalles de un integrante individual (bajo /{id}).
    // Por qué existe: Permite a las pantallas del voluntario y del supervisor visualizar y precargar los datos de los miembros familiares en el navegador.
    // Qué pasaría si no estuviera: No podríamos desplegar las tablas de integrantes familiares en la SPA.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Qué hace: Valida la sesión activa del voluntario.
        // Por qué existe: Salvaguarda los datos de identificación, celular, parentesco e historial de los integrantes familiares frente a accesos malintencionados.
        // Qué pasaría si no estuviera: Cualquier persona podría descargar listados de personas del censo (incluyendo menores de edad) sin estar autenticado.
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }
        
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Recurso no especificado.").toString());
            return;
        }
        
        String[] parts = pathInfo.split("/");
        
        try {
            if (parts[1].equals("familyPlan")) {
                if (parts.length < 3) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de plan familiar no provisto.").toString());
                    return;
                }
                
                // Caso A: members/familyPlan/select/{planId} (Para combos de coordinadores)
                if (parts.length > 3 && parts[2].equals("select")) {
                    int planId = Integer.parseInt(parts[3]);
                    
                    // Qué hace: Recupera un listado simplificado para selects de formulario.
                    // y luego de esto pasamos a IntegranteServicio.obtenerIntegrantesSeleccion, que lee de forma directa los nombres.
                    String resJson = servicio.obtenerIntegrantesSeleccion(planId);
                    response.getWriter().write(resJson);
                } 
                // Caso B: members/familyPlan/{planId} (Paginado para la tabla de integrantes)
                else {
                    int planId = Integer.parseInt(parts[2]);
                    String pageParam = request.getParameter("page");
                    int page = 1;
                    if (pageParam != null && !pageParam.isEmpty()) {
                        page = Integer.parseInt(pageParam);
                    }
                    
                    // Qué hace: Obtiene la lista paginada de integrantes familiares.
                    // y luego de esto pasamos a IntegranteServicio.listarIntegrantes, el cual realiza el SELECT correspondiente en la BD.
                    String resJson = servicio.listarIntegrantes(planId, page);
                    response.getWriter().write(resJson);
                }
            } else {
                int id = Integer.parseInt(parts[1]);
                
                // Qué hace: Consulta un integrante familiar por su ID único.
                // y luego de esto pasamos a IntegranteServicio.obtenerIntegrante, que hace SELECT filtrando por ID.
                String resJson = servicio.obtenerIntegrante(id);
                response.getWriter().write(resJson);
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "El identificador debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error del servidor: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Atiende peticiones POST para registrar un nuevo integrante familiar atado a un plan (bajo /{planId}).
    // Por qué existe: Permite persistir en la base de datos un nuevo miembro de la familia ingresado en el formulario.
    // Qué pasaría si no estuviera: Sería imposible registrar nuevos familiares dentro del censo familiar.
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }
        
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de plan familiar no provisto.").toString());
            return;
        }
        
        String[] parts = pathInfo.split("/");
        
        try {
            int planId = Integer.parseInt(parts[1]);
            
            StringBuilder buffer = new StringBuilder();
            String line;
            try (BufferedReader reader = request.getReader()) {
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
            }
            
            JSONObject json = new JSONObject(buffer.toString());
            IntegranteDTO dto = new IntegranteDTO();
            dto.setPlanId(planId);
            dto.setNames(json.getString("names"));
            dto.setLastNames(json.getString("last_names"));
            dto.setBirthDate(json.getString("birth_date"));
            dto.setDocumentNumber(json.optString("document_number", null));
            dto.setEps(json.optString("eps", null));
            dto.setPhone(json.optString("phone", null));
            
            dto.setDocumentTypeId(json.optInt("document_type_id", 0));
            dto.setKinshipId(json.optInt("kinship_id", 0));
            dto.setBloodGroupId(json.optInt("blood_group_id", 0));
            dto.setNationalityId(json.optInt("nationality_id", 0));
            dto.setGenderId(json.optInt("gender_id", 0));
            
            // Qué hace: Llama al servicio para guardar el integrante.
            // y luego de esto pasamos a IntegranteServicio.crearIntegrante, que valida e inserta el integrante en MySQL.
            String resJson = servicio.crearIntegrante(dto);
            response.getWriter().write(resJson);
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de plan familiar debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al registrar integrante: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Atiende peticiones HTTP PUT para actualizar de forma integral los datos de un integrante específico (bajo /{id}).
    // Por qué existe: Permite modificar la información demográfica o de contacto del integrante y guardarla de forma permanente.
    // Qué pasaría si no estuviera: No podríamos corregir nombres, teléfonos o datos demográficos ingresados incorrectamente.
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }
        
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de integrante no provisto.").toString());
            return;
        }
        
        String[] parts = pathInfo.split("/");
        
        try {
            int id = Integer.parseInt(parts[1]);
            
            StringBuilder buffer = new StringBuilder();
            String line;
            try (BufferedReader reader = request.getReader()) {
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
            }
            
            JSONObject json = new JSONObject(buffer.toString());
            IntegranteDTO dto = new IntegranteDTO();
            dto.setNames(json.getString("names"));
            dto.setLastNames(json.getString("last_names"));
            dto.setBirthDate(json.getString("birth_date"));
            dto.setDocumentNumber(json.optString("document_number", null));
            dto.setEps(json.optString("eps", null));
            dto.setPhone(json.optString("phone", null));
            
            dto.setDocumentTypeId(json.optInt("document_type_id", 0));
            dto.setKinshipId(json.optInt("kinship_id", 0));
            dto.setBloodGroupId(json.optInt("blood_group_id", 0));
            dto.setNationalityId(json.optInt("nationality_id", 0));
            dto.setGenderId(json.optInt("gender_id", 0));
            
            // Qué hace: Llama al servicio para actualizar los datos.
            // y luego de esto pasamos a IntegranteServicio.actualizarIntegrante, que ejecuta el UPDATE SQL en base de datos.
            String resJson = servicio.actualizarIntegrante(id, dto);
            response.getWriter().write(resJson);
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de integrante debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al actualizar integrante: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Atiende peticiones HTTP DELETE para eliminar de la base de datos un integrante familiar por su ID único (bajo /{id}).
    // Por qué existe: Permite dar de baja o quitar integrantes de la familia cuando el voluntario lo solicita desde la interfaz.
    // Qué pasaría si no estuviera: No podríamos retirar del censo familiar a personas registradas por equivocación.
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }
        
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de integrante no provisto.").toString());
            return;
        }
        
        String[] parts = pathInfo.split("/");
        
        try {
            int id = Integer.parseInt(parts[1]);
            
            // Qué hace: Elimina físicamente el integrante llamando al servicio.
            // y luego de esto pasamos a IntegranteServicio.eliminarIntegrante, que borra la fila en la BD.
            String resJson = servicio.eliminarIntegrante(id);
            response.getWriter().write(resJson);
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de integrante debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al eliminar integrante: " + e.getMessage()).toString());
        }
    }
}
