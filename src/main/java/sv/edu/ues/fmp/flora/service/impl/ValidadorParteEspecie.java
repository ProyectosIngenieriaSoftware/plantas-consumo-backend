package sv.edu.ues.fmp.flora.service.impl;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import sv.edu.ues.fmp.flora.entity.EspecieParteComestible;
import sv.edu.ues.fmp.flora.exception.EstadoInvalidoException;
import sv.edu.ues.fmp.flora.exception.RecursoNoEncontradoException;
import sv.edu.ues.fmp.flora.repository.EspecieParteComestibleRepository;

/**
 * Resuelve la parte comestible opcional y valida su pertenencia y estado.
 * Componente compartido por imágenes y futuros videos de especie.
 * Se utiliza dentro de la transacción del servicio consumidor.
 */
@Component
@RequiredArgsConstructor
public class ValidadorParteEspecie {
    private final EspecieParteComestibleRepository especieParteComestibleRepository;

    /** Devuelve null si no se indicó parte; rechaza partes inexistentes, ajenas o inactivas. */
    public EspecieParteComestible resolver(Long idEspecie, Long idEspecieParte) {
        if (idEspecieParte == null) return null;
        EspecieParteComestible parte = especieParteComestibleRepository.findById(idEspecieParte)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la parte comestible con id " + idEspecieParte));
        if (!parte.getEspecie().getIdEspecie().equals(idEspecie)) {
            throw new EstadoInvalidoException("La parte '" + parte.getPartePlanta().getNombre()
                    + "' pertenece a la especie '" + parte.getEspecie().getNombreCientifico()
                    + "', no a la especie solicitada con id " + idEspecie + ".");
        }
        if (!Boolean.TRUE.equals(parte.getActiva())) {
            throw new EstadoInvalidoException("La parte comestible '"
                    + parte.getPartePlanta().getNombre() + "' está desactivada.");
        }
        return parte;
    }
}
