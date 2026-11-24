package sv.edu.ues.fmp.flora.config;

import org.springframework.data.domain.PageRequest;

import sv.edu.ues.fmp.flora.entity.Usuario;
import sv.edu.ues.fmp.flora.repository.UsuarioRepository;

/**
 * El usuario al que pertenecen los tokens de las pruebas.
 *
 * <h2>Por que hizo falta</h2>
 * Las pruebas firmaban sus tokens con {@code .id(999L)}: un id inventado, que
 * no le importaba a nadie porque el {@code JwtDecoder} solo comprueba firma y
 * vigencia y nunca iba a la base por el usuario. Con
 * {@code VerificacionDeCuentaActiva} en la cadena eso dejo de ser gratis: el
 * filtro busca la cuenta del {@code sub} y, si no existe, responde 401. O sea
 * que <strong>esos tokens dejaron de ser validos a proposito</strong>, por el
 * mismo motivo por el que un token de una cuenta borrada tampoco lo es
 * (SEG-110, SEG-111, SEG-PB-07). Las pruebas se apoyaban en una ficcion que la
 * correccion ya no admite, asi que ahora firman a nombre de una cuenta real.
 *
 * <h2>Por que no se fija el id 1</h2>
 * {@code V3} siembra {@code admin} con el id 1 y seria lo mas corto, pero deja
 * la suite entera colgando de que esa cuenta siga activa --y desactivar
 * cuentas es justo lo que prueban varias de estas clases--. Se resuelve
 * consultando, igual que hace {@link SiembraDeFichas} con su
 * {@code responsable()}: la primera cuenta activa que haya, la que sea.
 *
 * <h2>Lo que estas pruebas siguen sin mirar</h2>
 * Los roles y los permisos del token siguen siendo los que cada prueba le pone
 * a mano, no los de la cuenta que firma. El filtro no los vuelve a consultar
 * --solo existencia y estado-- asi que una prueba puede seguir pidiendo un
 * token con {@code ESPECIE_CONSULTAR} y otro sin el, sobre la misma cuenta.
 */
public final class UsuariosDePrueba {

    private UsuariosDePrueba() {
    }

    /**
     * El id de una cuenta que existe y esta activa.
     *
     * @throws IllegalStateException si no hay ninguna. Sin cuentas activas no
     *                               se puede firmar un token que pase el
     *                               filtro, y es mejor decirlo asi que dejar
     *                               que la prueba falle con un 401 que no
     *                               explica nada.
     */
    /**
     * El id de una cuenta activa que no sea la que se indica.
     * <p>
     * Lo piden las pruebas del guard de propiedad de
     * {@code PUT /api/usuarios/{id}/clave}, que necesitan dos cosas a la vez:
     * un token de una cuenta <strong>real</strong> --o lo corta
     * {@code VerificacionDeCuentaActiva} con un 401 antes de llegar al
     * guard-- y que esa cuenta sea <strong>distinta</strong> de la que la
     * peticion ataca, porque si coincidieran el guard dejaria pasar y la
     * prueba medirian otra cosa.
     *
     * @throws IllegalStateException si no hay una segunda cuenta activa
     */
    public static Long idActivoDistintoDe(UsuarioRepository usuarioRepository, Long excluido) {
        return usuarioRepository.findByActivoTrue(PageRequest.of(0, 10))
                .stream()
                .map(Usuario::getIdUsuario)
                .filter(id -> !id.equals(excluido))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay ninguna cuenta activa distinta de la " + excluido
                                + ": estas pruebas necesitan dos para medir el guard de "
                                + "propiedad del cambio de clave."));
    }

    public static Long idActivo(UsuarioRepository usuarioRepository) {
        return usuarioRepository.findByActivoTrue(PageRequest.of(0, 1))
                .stream()
                .map(Usuario::getIdUsuario)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay ninguna cuenta activa en la base: los tokens de las pruebas "
                                + "necesitan una, porque VerificacionDeCuentaActiva comprueba "
                                + "que la cuenta del sub exista y siga activa."));
    }
}
