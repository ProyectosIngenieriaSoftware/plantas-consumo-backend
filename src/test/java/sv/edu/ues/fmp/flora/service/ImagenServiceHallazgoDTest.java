package sv.edu.ues.fmp.flora.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.fmp.flora.dto.response.ImagenEspecieResponse;
import sv.edu.ues.fmp.flora.dto.response.ImagenPreparacionResponse;
import sv.edu.ues.fmp.flora.entity.Especie;
import sv.edu.ues.fmp.flora.entity.ImagenEspecie;
import sv.edu.ues.fmp.flora.entity.ImagenPreparacion;
import sv.edu.ues.fmp.flora.entity.PreparacionConsumo;
import sv.edu.ues.fmp.flora.mapper.ImagenEspecieMapper;
import sv.edu.ues.fmp.flora.mapper.ImagenPreparacionMapper;
import sv.edu.ues.fmp.flora.repository.ImagenEspecieRepository;
import sv.edu.ues.fmp.flora.repository.ImagenPreparacionRepository;
import sv.edu.ues.fmp.flora.service.impl.ImagenEspecieServiceImpl;
import sv.edu.ues.fmp.flora.service.impl.ImagenPreparacionServiceImpl;
import sv.edu.ues.fmp.flora.service.impl.ValidadorParteEspecie;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImagenServiceHallazgoDTest {

    @Mock
    private ImagenEspecieRepository imagenEspecieRepository;

    @Mock
    private ValidadorParteEspecie validadorParteEspecie;

    @Mock
    private ImagenEspecieMapper imagenEspecieMapper;

    @InjectMocks
    private ImagenEspecieServiceImpl imagenEspecieService;

    @Mock
    private ImagenPreparacionRepository imagenPreparacionRepository;

    @Mock
    private ImagenPreparacionMapper imagenPreparacionMapper;

    @InjectMocks
    private ImagenPreparacionServiceImpl imagenPreparacionService;

    @Test
    void desactivarEspecie_debePonerActivaFalseYEsPrincipalFalse() {
        ImagenEspecie entidad = ImagenEspecie.builder()
                .idImagen(1L)
                .activa(true)
                .esPrincipal(true)
                .build();
        when(imagenEspecieRepository.findById(1L)).thenReturn(Optional.of(entidad));

        imagenEspecieService.desactivar(1L);

        assertFalse(entidad.getActiva(), "La imagen debe quedar inactiva (activa=false)");
        assertFalse(entidad.getEsPrincipal(), "La imagen debe perder condición de principal (esPrincipal=false)");
    }

    @Test
    void activarEspecie_noRobaPrincipalYRegresaComoNoPrincipal() {
        Especie especie = Especie.builder().idEspecie(10L).activa(true).build();
        ImagenEspecie entidad = ImagenEspecie.builder()
                .idImagen(1L)
                .especie(especie)
                .activa(false)
                .esPrincipal(false)
                .build();
        when(imagenEspecieRepository.findById(1L)).thenReturn(Optional.of(entidad));
        when(imagenEspecieMapper.toResponse(any())).thenAnswer(invocation -> {
            ImagenEspecie e = invocation.getArgument(0);
            return ImagenEspecieResponse.builder()
                    .idImagen(e.getIdImagen())
                    .activa(e.getActiva())
                    .esPrincipal(e.getEsPrincipal())
                    .build();
        });

        ImagenEspecieResponse response = imagenEspecieService.activar(1L);

        assertTrue(response.getActiva(), "La imagen reactivada debe estar activa");
        assertFalse(response.getEsPrincipal(), "La imagen reactivada debe ser no-principal (esPrincipal=false)");
        // No debe buscar ni desmarcar a la principal actual
        verify(imagenEspecieRepository, never()).findByEspecieIdEspecieAndEsPrincipalTrueAndActivaTrue(anyLong());
    }

    @Test
    void desactivarPreparacion_debePonerActivaFalseYEsPrincipalFalse() {
        ImagenPreparacion entidad = ImagenPreparacion.builder()
                .idImagenPreparacion(2L)
                .activa(true)
                .esPrincipal(true)
                .build();
        when(imagenPreparacionRepository.findById(2L)).thenReturn(Optional.of(entidad));

        imagenPreparacionService.desactivar(2L);

        assertFalse(entidad.getActiva(), "La imagen debe quedar inactiva (activa=false)");
        assertFalse(entidad.getEsPrincipal(), "La imagen debe perder condición de principal (esPrincipal=false)");
    }

    @Test
    void activarPreparacion_noRobaPrincipalYRegresaComoNoPrincipal() {
        PreparacionConsumo preparacion = PreparacionConsumo.builder().idPreparacion(20L).activa(true).build();
        ImagenPreparacion entidad = ImagenPreparacion.builder()
                .idImagenPreparacion(2L)
                .preparacion(preparacion)
                .activa(false)
                .esPrincipal(false)
                .build();
        when(imagenPreparacionRepository.findById(2L)).thenReturn(Optional.of(entidad));
        when(imagenPreparacionMapper.toResponse(any())).thenAnswer(invocation -> {
            ImagenPreparacion e = invocation.getArgument(0);
            return ImagenPreparacionResponse.builder()
                    .idImagenPreparacion(e.getIdImagenPreparacion())
                    .activa(e.getActiva())
                    .esPrincipal(e.getEsPrincipal())
                    .build();
        });

        ImagenPreparacionResponse response = imagenPreparacionService.activar(2L);

        assertTrue(response.getActiva(), "La imagen reactivada debe estar activa");
        assertFalse(response.getEsPrincipal(), "La imagen reactivada debe ser no-principal (esPrincipal=false)");
        // No debe buscar ni desmarcar a la principal actual
        verify(imagenPreparacionRepository, never()).findByPreparacionIdPreparacionAndEsPrincipalTrueAndActivaTrue(anyLong());
    }
}

