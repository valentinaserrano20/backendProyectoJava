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

// Qué hace: Servlet encargado de mapear las peticiones HTTP CRUD (GET, POST, PUT, DELETE) sobre la entidad integrantes (miembros de la familia).
// Por qué existe: Actúa como el controlador de entrada para gestionar el flujo de datos de los integrantes de un plan de emergencia.
// Qué problema resuelve: Enruta las peticiones de red del frontend hacia el servicio de negocio correspondiente, garantizando la seguridad mediante la sesión activa.
@WebServlet("/api/members/*")
public class IntegranteServlet extends HttpServlet {
    private final IntegranteServicio servicio = new IntegranteServicio();

    // Qué hace: Atiende peticiones GET para listar integrantes de un plan (bajo /familyPlan/{planId}) o consultar los detalles de un integrante individual (bajo /{id}).
    // Por qué existe: Permite a las pantallas del voluntario y del supervisor visualizar y precargar los datos de los miembros familiares en el navegador.
    // Qué problema resuelve: Provee acceso de lectura parametrizado y paginado a la información de integrantes en formato JSON.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
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
                
                // Caso A: members/familyPlan/select/{planId}
                if (parts.length > 3 && parts[2].equals("select")) {
                    int planId = Integer.parseInt(parts[3]);
                    String resJson = servicio.obtenerIntegrantesSeleccion(planId);
                    response.getWriter().write(resJson);
                } 
                // Caso B: members/familyPlan/{planId} (paginado)
                else {
                    int planId = Integer.parseInt(parts[2]);
                    String pageParam = request.getParameter("page");
                    int page = 1;
                    if (pageParam != null && !pageParam.isEmpty()) {
                        page = Integer.parseInt(pageParam);
                    }
                    String resJson = servicio.listarIntegrantes(planId, page);
                    response.getWriter().write(resJson);
                }
            } else {
                int id = Integer.parseInt(parts[1]);
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
    // Qué problema resuelve: Recibe el payload JSON del cuerpo de la petición, deserializa las llaves y delega el registro al servicio de negocio.
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
    // Qué problema resuelve: Lee el cuerpo de la solicitud JSON, asigna los datos actualizados y ejecuta el UPDATE a través de la capa de servicio.
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
    // Qué problema resuelve: Enruta el borrado físico del registro en base de datos de manera transaccional, eliminando previamente registros hijos.
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
