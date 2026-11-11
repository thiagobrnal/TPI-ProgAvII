package com.gestionamateur.tpi.progav.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Modelo Categoria (INF-03R)")
public class CategoriaTest {

    @Test
    @DisplayName("Debe permitir edad máxima NULL para categorías abiertas (Libre y Senior)")
    void testCategoriaEdadMaximaNull() throws TorneoExcepcion {
        Categoria libre = new Categoria("Libre", "Masculino", 16, null);
        assertNotNull(libre);
        assertEquals(16, libre.getEdadMinima());
        assertNull(libre.getEdadMaxima());
        assertEquals("Libre", libre.getNombre());
        assertEquals("Masculino", libre.getGenero());

        Categoria senior = new Categoria("Senior", "Masculino", 35, null);
        assertNotNull(senior);
        assertEquals(35, senior.getEdadMinima());
        assertNull(senior.getEdadMaxima());
    }

    @Test
    @DisplayName("Debe permitir categoría acotada con edad mínima y máxima (Sub-20)")
    void testCategoriaAcotada() throws TorneoExcepcion {
        Categoria sub20 = new Categoria("Sub-20", "Masculino", 16, 20);
        assertNotNull(sub20);
        assertEquals(16, sub20.getEdadMinima());
        assertEquals(Integer.valueOf(20), sub20.getEdadMaxima());
    }

    @Test
    @DisplayName("Debe rechazar edad mínima menor o igual a cero")
    void testEdadMinimaInvalida() {
        assertThrows(TorneoExcepcion.class, () -> {
            new Categoria("Infantil", "Mixto", 0, 12);
        });
        assertThrows(TorneoExcepcion.class, () -> {
            new Categoria("Negativo", "Mixto", -5, 12);
        });
    }

    @Test
    @DisplayName("Debe rechazar cuando edad máxima es menor que edad mínima")
    void testRangoEdadesIncoherente() {
        assertThrows(TorneoExcepcion.class, () -> {
            new Categoria("Invalida", "Masculino", 25, 20);
        });
    }

    @Test
    @DisplayName("Debe rechazar nombre de categoría vacío")
    void testNombreVacio() {
        assertThrows(TorneoExcepcion.class, () -> {
            new Categoria("", "Masculino", 16, 20);
        });
    }
}
