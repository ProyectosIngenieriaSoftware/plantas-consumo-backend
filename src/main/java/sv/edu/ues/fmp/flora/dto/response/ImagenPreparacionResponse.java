package sv.edu.ues.fmp.flora.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Representacion publica de una imagen de preparacion. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImagenPreparacionResponse {

    private Long idImagenPreparacion;
    private Long idPreparacion;
    private String urlImagen;
    private String titulo;
    private String descripcion;
    private String textoAlternativo;
    private String creditos;
    private Boolean esPrincipal;
    private Boolean activa;
    private LocalDateTime fechaRegistro;
}

