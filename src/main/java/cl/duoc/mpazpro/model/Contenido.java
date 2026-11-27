package cl.duoc.mpazpro.model;

import jakarta.persistence.*;

@Entity
@Table(name = "contenidos")
public class Contenido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String asignaturaId;

    @Column(nullable = false)
    private String unidad;

    private String codigoOA;

    @Column(nullable = false, length = 1000)
    private String descripcionOA;

    @Column(nullable = false, length = 2000)
    private String explicacion;

    @Column(nullable = false)
    private String tituloActividad;

    @Column(nullable = false, length = 1000)
    private String pregunta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoContenido estado;

    @Column(length = 1000)
    private String observacion;

    @Column(nullable = false)
    private String autorCorreo;

    @Column(nullable = false)
    private String autorPerfil;

    private String actualizado;

    // Archivo adjunto (imagen o audio)
    private String archivoNombre;
    private String archivoTipo;
    private String archivoRuta;

    public Contenido() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAsignaturaId() { return asignaturaId; }
    public void setAsignaturaId(String asignaturaId) { this.asignaturaId = asignaturaId; }

    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }

    public String getCodigoOA() { return codigoOA; }
    public void setCodigoOA(String codigoOA) { this.codigoOA = codigoOA; }

    public String getDescripcionOA() { return descripcionOA; }
    public void setDescripcionOA(String descripcionOA) { this.descripcionOA = descripcionOA; }

    public String getExplicacion() { return explicacion; }
    public void setExplicacion(String explicacion) { this.explicacion = explicacion; }

    public String getTituloActividad() { return tituloActividad; }
    public void setTituloActividad(String tituloActividad) { this.tituloActividad = tituloActividad; }

    public String getPregunta() { return pregunta; }
    public void setPregunta(String pregunta) { this.pregunta = pregunta; }

    public EstadoContenido getEstado() { return estado; }
    public void setEstado(EstadoContenido estado) { this.estado = estado; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public String getAutorCorreo() { return autorCorreo; }
    public void setAutorCorreo(String autorCorreo) { this.autorCorreo = autorCorreo; }

    public String getAutorPerfil() { return autorPerfil; }
    public void setAutorPerfil(String autorPerfil) { this.autorPerfil = autorPerfil; }

    public String getActualizado() { return actualizado; }
    public void setActualizado(String actualizado) { this.actualizado = actualizado; }

    public String getArchivoNombre() { return archivoNombre; }
    public void setArchivoNombre(String archivoNombre) { this.archivoNombre = archivoNombre; }

    public String getArchivoTipo() { return archivoTipo; }
    public void setArchivoTipo(String archivoTipo) { this.archivoTipo = archivoTipo; }

    public String getArchivoRuta() { return archivoRuta; }
    public void setArchivoRuta(String archivoRuta) { this.archivoRuta = archivoRuta; }
}