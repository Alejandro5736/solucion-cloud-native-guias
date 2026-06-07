package com.cloudnative.Semana3.model;

import lombok.Data;

@Data
public class GuiaRequest {
    private String id;
    private String transportista;
    private String fecha; // Formato requerido: YYYYMM (Ej: "20211" o "202606")
    private String contenido; // Texto o cuerpo simulado que se escribirá en el PDF
}