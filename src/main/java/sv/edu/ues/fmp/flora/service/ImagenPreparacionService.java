package sv.edu.ues.fmp.flora.service;

import java.util.List;

import sv.edu.ues.fmp.flora.dto.request.ImagenPreparacionRequest;
import sv.edu.ues.fmp.flora.dto.response.ImagenPreparacionResponse;


public interface ImagenPreparacionService {

    List<ImagenPreparacionResponse> listarPorPreparacion(Long idPreparacion, boolean soloActivas);

    ImagenPreparacionResponse obtenerPorId(Long id);

    ImagenPreparacionResponse crear(Long idPreparacion, ImagenPreparacionRequest request);

    ImagenPreparacionResponse actualizar(Long id, ImagenPreparacionRequest request);

    void desactivar(Long id);

    ImagenPreparacionResponse activar(Long id);

    ImagenPreparacionResponse marcarComoPrincipal(Long id);
}

