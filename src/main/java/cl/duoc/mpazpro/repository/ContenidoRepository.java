package cl.duoc.mpazpro.repository;

import cl.duoc.mpazpro.model.Contenido;
import cl.duoc.mpazpro.model.EstadoContenido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ContenidoRepository extends JpaRepository<Contenido, Long> {

    List<Contenido> findByEstado(EstadoContenido estado);

    List<Contenido> findByEstadoInOrAutorCorreo(Collection<EstadoContenido> estados, String autorCorreo);
}