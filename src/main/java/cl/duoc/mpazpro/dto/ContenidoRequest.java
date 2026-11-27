package cl.duoc.mpazpro.dto;

public record ContenidoRequest(
        String asignaturaId,
        String unidad,
        String codigoOA,
        String descripcionOA,
        String explicacion,
        String tituloActividad,
        String pregunta
) {}