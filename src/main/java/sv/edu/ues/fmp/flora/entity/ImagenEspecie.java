package sv.edu.ues.fmp.flora.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import sv.edu.ues.fmp.flora.entity.enums.OrigenImagen;
import sv.edu.ues.fmp.flora.entity.enums.TipoImagen;

/**
 * Imagen de una especie, con baja lógica. El binario puede ser un enlace
 * externo o un archivo propio guardado en el servidor; lo dice {@link #origen}.
 * La base garantiza una principal activa mediante uk_imagen_principal_especie.
 * Las dos relaciones simples comparten la validación de pertenencia de la FK
 * compuesta fk_imagen_parte_especie; el servicio la comprueba antes de guardar.
 *
 * <h2>Las dos fuentes y por que son excluyentes</h2>
 * {@code ck_imagen_fuente_unica} (V22) exige exactamente una: una fila URL
 * tiene {@code url_imagen} y {@code ruta_archivo} en null, y una fila PROPIA lo
 * contrario. Ninguna de las dos columnas puede ser NOT NULL por separado, asi
 * que es ese CHECK --y no la nulabilidad-- lo que impide la fila sin fuente y
 * la fila con dos. Si alguna vez hubiera que relajarlo, nada en Java garantiza
 * que la imagen se pueda mostrar.
 *
 * <h2>El archivo no lo borra nadie</h2>
 * La baja lógica marca la fila y deja el binario en disco, de modo que
 * reactivar la imagen la vuelve a servir sin tener que volver a subirla.
 * {@code ruta_archivo} se conserva igual en una fila inactiva.
 *
 * <h2>El trigger de toque</h2>
 * {@code trg_imagen_especie_toca_especie} (V23) actualiza la especie dueña en
 * cada INSERT, UPDATE o DELETE de esta tabla, para que el BEFORE UPDATE de
 * {@code especie} refresque su {@code fecha_actualizacion}. Nada de eso se ve
 * desde aquí: no hay código Java que lo haga, y por eso está escrito.
 */
@Entity
@Table(name = "imagen_especie")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImagenEspecie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_imagen", nullable = false, updatable = false)
    private Long idImagen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_especie", nullable = false, updatable = false)
    private Especie especie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_especie_parte")
    private EspecieParteComestible especieParte;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_imagen", nullable = false, length = 20)
    private TipoImagen tipoImagen;

    /**
     * Pasa a admitir null en V22: solo lo lleva una imagen de origen URL. En
     * una imagen PROPIA es null y el CHECK lo exige asi.
     */
    @Column(name = "url_imagen", length = 1000)
    private String urlImagen;

    /**
     * Si el binario es un enlace externo o un archivo nuestro. Sin
     * {@code @Builder.Default} a proposito: la imagen no tiene un origen por
     * defecto razonable --quien la crea sabe cual es-- y dejarlo en null lo
     * hace fallar en la base en vez de guardar una fila mal clasificada.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "origen", nullable = false, length = 6)
    private OrigenImagen origen;

    /**
     * Nombre del archivo en la carpeta de almacenamiento, generado por el
     * sistema: un UUID mas la extension que corresponde al tipo detectado.
     * Nunca el nombre que mando el cliente, que ni es unico ni es de fiar --un
     * {@code ../../application.properties} saldria de la carpeta--. Solo lo
     * lleva una imagen PROPIA.
     * <p>
     * Es un nombre de archivo, no una ruta: la carpeta sale de la configuracion
     * y puede cambiar sin tocar ninguna fila. Guardar aqui la ruta absoluta
     * ataria los datos a la maquina donde se subieron.
     */
    @Column(name = "ruta_archivo", length = 120)
    private String rutaArchivo;

    /**
     * Como se llamaba el archivo en la maquina de quien lo subio. Se guarda
     * aparte, solo para mostrarlo: nunca se usa para construir una ruta ni para
     * deducir el tipo.
     */
    @Column(name = "nombre_original", length = 255)
    private String nombreOriginal;

    /**
     * El tipo real del archivo, el que se determino leyendo sus primeros bytes
     * y no el que declaro el cliente. Es lo que se devuelve como
     * {@code Content-Type} al servirlo.
     */
    @Column(name = "tipo_contenido", length = 100)
    private String tipoContenido;

    /** Tamaño del archivo en disco, para listarlo sin tener que ir a leerlo. */
    @Column(name = "tamanio_bytes")
    private Long tamanioBytes;

    @Column(name = "titulo", length = 200)
    private String titulo;

    @Column(name = "descripcion", columnDefinition = "text")
    private String descripcion;

    @Column(name = "texto_alternativo", nullable = false, length = 300)
    private String textoAlternativo;

    @Column(name = "creditos", length = 250)
    private String creditos;

    @Builder.Default
    @Column(name = "es_principal", nullable = false)
    private Boolean esPrincipal = false;

    @Builder.Default
    @Column(name = "activa", nullable = false)
    private Boolean activa = true;

    /** PostgreSQL asigna CURRENT_TIMESTAMP y Hibernate recupera el valor generado. */
    @Generated(event = EventType.INSERT)
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;
}
