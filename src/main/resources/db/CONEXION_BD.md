# Guía de Conexión y Despliegue de Base de Datos PostgreSQL — TPI-PA2

**Proyecto:** Sistema de Gestión de Torneos de Fútbol Amateur  
**Asignatura:** Programación Avanzada II — UGD  
**Alcance:** Release 1 (Aplicación Desktop Java Swing / JDBC nativo / PostgreSQL)  
**Fecha:** Noviembre 2026  

---

## 1. Arquitectura de Datos y Alcance

El sistema opera bajo un modelo de persistencia relacional estricta en **PostgreSQL** mediante controladores JDBC nativos, sin frameworks ORM pesados, garantizando máxima fidelidad al modelo relacional de 17 tablas y 22 restricciones de clave foránea.

Los scripts maestros canónicos residen en:
- DDL Estructural: `src/main/resources/db/schema.sql`
- Semillas de Configuración y Catálogos: `src/main/resources/db/seed_data.sql`

Ambos scripts son **completamente idempotentes y no destructivos**: no contienen sentencias `DROP TABLE`, `DROP DATABASE` ni `DROP SCHEMA`.

---

## 2. Entornos de Conexión

### A. Entorno Local de Validación (Notebook Secundaria — Fase INF-03)
Durante la fase de consolidación e integración INF-03, se utilizó una instancia PostgreSQL 17.2 portable aislada, configurada para no alterar el sistema operativo anfitrión:
- **Host:** `127.0.0.1`
- **Puerto:** `55432`
- **Base de datos:** `tpi_pa2_inf03_validation`
- **Usuario:** `postgres`
- **Contraseña:** *(sin contraseña en socket local trust)*
- **JDBC Connection String:**  
  `jdbc:postgresql://127.0.0.1:55432/tpi_pa2_inf03_validation`
- **Ubicación de binarios portables:** `%LOCALAPPDATA%\TPI-PA2\tools\postgresql`
- **Cluster de datos:** `%LOCALAPPDATA%\TPI-PA2\postgres-data`

### B. Entorno Estándar de Desarrollo y Producción (PC Principal / Despliegue Final)
Para el desarrollo regular y la entrega final en la PC Principal:
- **Host:** `localhost` / `127.0.0.1`
- **Puerto predeterminado:** `5432`
- **Base de datos sugerida:** `tpi_pa2` o `tpi_pa2_dev`
- **Usuario:** `postgres` (o el usuario administrador configurado localmente)
- **JDBC Connection String:**  
  `jdbc:postgresql://localhost:5432/tpi_pa2`

---

## 3. Procedimiento de Inicialización de Base de Datos

Para inicializar una nueva base de datos limpia desde la línea de comandos o desde pgAdmin:

### Paso 1: Crear la base de datos
```sql
CREATE DATABASE tpi_pa2 WITH ENCODING = 'UTF8';
```

### Paso 2: Ejecutar el DDL de estructura (`schema.sql`)
```bash
psql -h localhost -p 5432 -U postgres -d tpi_pa2 -v ON_ERROR_STOP=1 -f src/main/resources/db/schema.sql
```
*Crea las 17 tablas, sus índices de optimización y las 22 restricciones referenciales con reglas `ON DELETE RESTRICT`.*

### Paso 3: Ejecutar los datos semilla (`seed_data.sql`)
```bash
psql -h localhost -p 5432 -U postgres -d tpi_pa2 -v ON_ERROR_STOP=1 -f src/main/resources/db/seed_data.sql
```
*Carga los 5 roles del sistema, las 3 configuraciones de negocio, las 9 categorías oficiales (Sub-20, Libre y Senior en Masculino, Femenino y Mixto) y los 4 motivos formales de suspensión.*

---

## 4. Lineamientos para la Capa DAO (INF-04)

1. **Gestión de Conexiones:** Centralizar la apertura y cierre en una clase `ConexionBD` o `DatabaseConnection` con patrón Singleton / Factory.
2. **Configuración Externa:** Cargar los parámetros desde un archivo de propiedades (`database.properties` en `src/main/resources`) o variables de entorno:
   - `DB_HOST` (default: `127.0.0.1`)
   - `DB_PORT` (default: `5432`, o `55432` para validación)
   - `DB_NAME` (default: `tpi_pa2`)
   - `DB_USER` (default: `postgres`)
   - `DB_PASS`
3. **Manejo Transaccional en Cierre de Partido (CU-04 / CU-12):** Seguir estrictamente la secuencia de 11 pasos documentada en `PLAN_CONSOLIDACION_INF03_CANONICO.md` utilizando `connection.setAutoCommit(false)`, `commit()` y `rollback()`.
