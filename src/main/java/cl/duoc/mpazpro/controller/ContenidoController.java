package cl.duoc.mpazpro.controller;

import cl.duoc.mpazpro.dto.CambioEstadoRequest;
import cl.duoc.mpazpro.dto.ContenidoRequest;
import cl.duoc.mpazpro.model.Contenido;
import cl.duoc.mpazpro.model.EstadoContenido;
import cl.duoc.mpazpro.repository.ContenidoRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/contenidos")
public class ContenidoController {

    private static final Set<String> ASIGNATURAS =
            Set.of("lenguaje", "matematica", "ciencias", "historia");

    private static final Path CARPETA_ARCHIVOS = Path.of("uploads");

    private final ContenidoRepository repositorio;

    public ContenidoController(ContenidoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @GetMapping
    public List<Contenido> listar(Authentication auth) {
        if (tieneRol(auth, "utp")) {
            return repositorio.findAll();
        }
        if (tieneRol(auth, "docente")) {
            return repositorio.findByEstadoInOrAutorCorreo(
                    List.of(EstadoContenido.AUTORIZADO, EstadoContenido.PUBLICADO),
                    auth.getName());
        }
        return repositorio.findByEstado(EstadoContenido.PUBLICADO);
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody ContenidoRequest d, Authentication auth) {
        if (d.asignaturaId() == null || !ASIGNATURAS.contains(d.asignaturaId())
                || vacio(d.unidad()) || vacio(d.descripcionOA()) || vacio(d.explicacion())
                || vacio(d.tituloActividad()) || vacio(d.pregunta())) {
            return error(HttpStatus.BAD_REQUEST, "Faltan datos obligatorios del contenido");
        }

        Contenido c = new Contenido();
        c.setAsignaturaId(d.asignaturaId());
        c.setUnidad(d.unidad().trim());
        c.setCodigoOA(d.codigoOA() == null ? "" : d.codigoOA().trim());
        c.setDescripcionOA(d.descripcionOA().trim());
        c.setExplicacion(d.explicacion().trim());
        c.setTituloActividad(d.tituloActividad().trim());
        c.setPregunta(d.pregunta().trim());
        c.setAutorCorreo(auth.getName());

        if (tieneRol(auth, "docente")) {
            c.setAutorPerfil("docente");
            c.setEstado(EstadoContenido.EN_REVISION);
        } else {
            c.setAutorPerfil("utp");
            c.setEstado(EstadoContenido.BORRADOR);
        }
        c.setActualizado(ahora());

        return ResponseEntity.status(HttpStatus.CREATED).body(repositorio.save(c));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Long id, @RequestBody ContenidoRequest d) {
        Contenido c = repositorio.findById(id).orElse(null);
        if (c == null) {
            return error(HttpStatus.NOT_FOUND, "Contenido no encontrado");
        }
        if (c.getEstado() != EstadoContenido.BORRADOR && c.getEstado() != EstadoContenido.RECHAZADO) {
            return error(HttpStatus.CONFLICT, "Solo se puede editar un borrador o un contenido rechazado");
        }
        if (d.asignaturaId() == null || !ASIGNATURAS.contains(d.asignaturaId())
                || vacio(d.unidad()) || vacio(d.descripcionOA()) || vacio(d.explicacion())
                || vacio(d.tituloActividad()) || vacio(d.pregunta())) {
            return error(HttpStatus.BAD_REQUEST, "Faltan datos obligatorios del contenido");
        }

        c.setAsignaturaId(d.asignaturaId());
        c.setUnidad(d.unidad().trim());
        c.setCodigoOA(d.codigoOA() == null ? "" : d.codigoOA().trim());
        c.setDescripcionOA(d.descripcionOA().trim());
        c.setExplicacion(d.explicacion().trim());
        c.setTituloActividad(d.tituloActividad().trim());
        c.setPregunta(d.pregunta().trim());
        c.setEstado(EstadoContenido.BORRADOR);
        c.setObservacion("");
        c.setActualizado(ahora());
        return ResponseEntity.ok(repositorio.save(c));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @RequestBody CambioEstadoRequest d) {
        Contenido c = repositorio.findById(id).orElse(null);
        if (c == null) {
            return error(HttpStatus.NOT_FOUND, "Contenido no encontrado");
        }

        String accion = d.accion() == null ? "" : d.accion();
        EstadoContenido desde = c.getEstado();
        EstadoContenido hacia;

        switch (accion) {
            case "enviar-revision" -> hacia = desde == EstadoContenido.BORRADOR ? EstadoContenido.EN_REVISION : null;
            case "aprobar" -> hacia = desde == EstadoContenido.EN_REVISION ? EstadoContenido.AUTORIZADO : null;
            case "rechazar" -> hacia = desde == EstadoContenido.EN_REVISION ? EstadoContenido.RECHAZADO : null;
            case "publicar" -> hacia = desde == EstadoContenido.AUTORIZADO ? EstadoContenido.PUBLICADO : null;
            case "deshabilitar" -> hacia = desde == EstadoContenido.PUBLICADO ? EstadoContenido.DESHABILITADO : null;
            default -> {
                return error(HttpStatus.BAD_REQUEST, "Acción desconocida");
            }
        }

        if (hacia == null) {
            return error(HttpStatus.CONFLICT, "No se puede '" + accion + "' desde el estado " + desde);
        }
        if (hacia == EstadoContenido.RECHAZADO && vacio(d.observacion())) {
            return error(HttpStatus.BAD_REQUEST, "Para rechazar debes indicar una observación");
        }

        c.setEstado(hacia);
        c.setObservacion(d.observacion() == null ? "" : d.observacion().trim());
        c.setActualizado(ahora());
        return ResponseEntity.ok(repositorio.save(c));
    }

    // ---------- Archivos adjuntos (imagen o audio) ----------

    @PostMapping("/{id}/archivo")
    public ResponseEntity<?> subirArchivo(@PathVariable Long id,
                                          @RequestParam("archivo") MultipartFile archivo,
                                          Authentication auth) throws IOException {
        Contenido c = repositorio.findById(id).orElse(null);
        if (c == null) {
            return error(HttpStatus.NOT_FOUND, "Contenido no encontrado");
        }

        boolean esUtp = tieneRol(auth, "utp");
        boolean esAutor = c.getAutorCorreo().equals(auth.getName());
        boolean editable = c.getEstado() == EstadoContenido.BORRADOR
                || c.getEstado() == EstadoContenido.EN_REVISION
                || c.getEstado() == EstadoContenido.RECHAZADO;

        if (!esUtp && !(esAutor && editable)) {
            return error(HttpStatus.FORBIDDEN, "No puedes modificar el archivo de este contenido");
        }
        if (archivo.isEmpty()) {
            return error(HttpStatus.BAD_REQUEST, "El archivo está vacío");
        }

        String tipo = archivo.getContentType();
        if (tipo == null || !(tipo.startsWith("image/") || tipo.startsWith("audio/"))) {
            return error(HttpStatus.BAD_REQUEST, "Solo se permiten imágenes o audios");
        }

        Files.createDirectories(CARPETA_ARCHIVOS);
        if (c.getArchivoRuta() != null) {
            Files.deleteIfExists(CARPETA_ARCHIVOS.resolve(c.getArchivoRuta()));
        }

        String nombreDisco = UUID.randomUUID() + extension(archivo.getOriginalFilename());
        Files.copy(archivo.getInputStream(), CARPETA_ARCHIVOS.resolve(nombreDisco),
                StandardCopyOption.REPLACE_EXISTING);

        c.setArchivoNombre(archivo.getOriginalFilename());
        c.setArchivoTipo(tipo);
        c.setArchivoRuta(nombreDisco);
        c.setActualizado(ahora());
        return ResponseEntity.ok(repositorio.save(c));
    }

    @GetMapping("/{id}/archivo")
    public ResponseEntity<?> descargarArchivo(@PathVariable Long id, Authentication auth) {
        Contenido c = repositorio.findById(id).orElse(null);
        if (c == null || c.getArchivoRuta() == null) {
            return error(HttpStatus.NOT_FOUND, "Archivo no encontrado");
        }

        boolean esUtp = tieneRol(auth, "utp");
        boolean esAutor = c.getAutorCorreo().equals(auth.getName());
        boolean publicado = c.getEstado() == EstadoContenido.PUBLICADO;
        boolean autorizado = c.getEstado() == EstadoContenido.AUTORIZADO;

        boolean permitido = esUtp || publicado
                || (tieneRol(auth, "docente") && (esAutor || autorizado));
        if (!permitido) {
            return error(HttpStatus.FORBIDDEN, "No tienes acceso a este archivo");
        }

        Path ruta = CARPETA_ARCHIVOS.resolve(c.getArchivoRuta());
        if (!Files.exists(ruta)) {
            return error(HttpStatus.NOT_FOUND, "El archivo ya no está en el servidor");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(c.getArchivoTipo()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"contenido-" + c.getId() + extension(c.getArchivoNombre()) + "\"")
                .body(new FileSystemResource(ruta));
    }

    // ---------- Utilidades ----------

    private String extension(String nombre) {
        if (nombre == null) {
            return "";
        }
        int punto = nombre.lastIndexOf('.');
        if (punto < 0) {
            return "";
        }
        String ext = nombre.substring(punto + 1).toLowerCase().replaceAll("[^a-z0-9]", "");
        return (ext.isEmpty() || ext.length() > 5) ? "" : "." + ext;
    }

    private boolean tieneRol(Authentication auth, String rol) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + rol));
    }

    private boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }

    private String ahora() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("error", mensaje));
    }
}