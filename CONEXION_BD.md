# Guía de Configuración y Conexión JDBC — PostgreSQL Local (Release 1)

Este documento describe cómo configurar los parámetros de conexión hacia el servidor **PostgreSQL local** utilizado por la aplicación de escritorio en **Release 1 (TPI-PA2)** a través del componente centralizado [`ConexionBD`](src/main/java/com/gestionamateur/tpi/progav/dao/ConexionBD.java).

---

## 1. Variables de entorno requeridas

Por seguridad e integridad del repositorio, la aplicación **no almacena credenciales en el código fuente ni en archivos versionables**. La clase `com.gestionamateur.tpi.progav.dao.ConexionBD` exige que estén definidas las siguientes tres variables de entorno:

| Variable de entorno | Obligatoria | Descripción | Valor / Formato de referencia |
| :--- | :---: | :--- | :--- |
| `TPI_DB_URL` | Sí | URL de conexión JDBC hacia PostgreSQL local | `jdbc:postgresql://localhost:5432/tpi_pa2` |
| `TPI_DB_USER` | Sí | Usuario del motor PostgreSQL local | Ej.: `postgres` (o el rol local asignado) |
| `TPI_DB_PASSWORD` | Sí | Contraseña del usuario PostgreSQL local | `<tu_clave_local>` (nunca versionar) |

> **Nota arquitectónica sobre la configuración:** En etapas previas de diseño se contempló el uso de un archivo `database.properties`. La decisión arquitectónica vigente adoptó variables de entorno (`TPI_DB_URL`, `TPI_DB_USER`, `TPI_DB_PASSWORD`) para evitar el versionado accidental de credenciales locales, mejorar la seguridad y facilitar la ejecución en múltiples puestos de trabajo sin riesgo de colisión en Git.

---

## 2. Formato de la URL JDBC y base de datos de referencia

- **Esquema y protocolo:** `jdbc:postgresql://<host>:<puerto>/<nombre_base>`
- **Servidor local:** `localhost` (puerto por defecto `5432`).
- **Base de datos de referencia:** `tpi_pa2`.
- **URL completa de referencia:**
  ```text
  jdbc:postgresql://localhost:5432/tpi_pa2
  ```

---

## 3. Configuración temporal en PowerShell (sesión actual)

Antes de ejecutar la comprobación de conectividad o lanzar la aplicación desde la terminal o el IDE en la misma sesión, definí las variables de entorno en **PowerShell** únicamente para la sesión actual (reemplazando los marcadores por tus valores locales):

```powershell
$env:TPI_DB_URL = "jdbc:postgresql://localhost:5432/tpi_pa2"
$env:TPI_DB_USER = "<tu_usuario_local>"
$env:TPI_DB_PASSWORD = "<tu_clave_local>"
```

> **Importante:**
> - Estas asignaciones (`$env:...`) rigen únicamente mientras permanezca abierta la ventana actual de PowerShell.
> - **No utilices `setx`** ni modifiques variables de entorno globales/persistentes de Windows en equipos compartidos.
> - Para limpiar las variables al finalizar la sesión en la misma consola:
>   ```powershell
>   Remove-Item Env:\TPI_DB_URL, Env:\TPI_DB_USER, Env:\TPI_DB_PASSWORD -ErrorAction SilentlyContinue
>   ```

---

## 4. Comprobación controlada de conexión

### Opción A — Desde línea de comandos con Maven

Con las variables `$env:TPI_DB_URL`, `$env:TPI_DB_USER` y `$env:TPI_DB_PASSWORD` definidas en la sesión actual de PowerShell y el servidor PostgreSQL iniciado, ubicarse en el directorio del proyecto (`03_Trabajo\TPI-ProgAvII`) y ejecutar:

```powershell
mvn compile exec:java "-Dexec.mainClass=com.gestionamateur.tpi.progav.dao.ConexionBD"
```

Si la configuración y el servidor local son correctos, el comando ejecutará `SELECT 1`, cerrará la conexión y mostrará:

```text
Iniciando comprobación controlada de conexión PostgreSQL local...
OK: Conexión verificada exitosamente contra 'jdbc:postgresql://localhost:5432/tpi_pa2' con el usuario '<tu_usuario_local>'.
```

### Opción B — Uso programático desde los DAOs

Cada invocación a `ConexionBD.obtenerConexion()` entrega una nueva instancia de `java.sql.Connection` que debe cerrarse mediante `try-with-resources`:

```java
try (Connection conn = ConexionBD.obtenerConexion()) {
    // Ejecución de PreparedStatement / consultas JDBC
}
```

Para verificar únicamente la disponibilidad del motor desde código:

```java
boolean disponible = ConexionBD.comprobarConexion();
```

---

## 5. Errores habituales y diagnóstico

1. **Variables de entorno faltantes o vacías:**
   - *Mensaje:* `Configuración de base de datos incompleta. Faltan las siguientes variables de entorno obligatorias: ...`
   - *Causa:* Una o más variables (`TPI_DB_URL`, `TPI_DB_USER`, `TPI_DB_PASSWORD`) no fueron definidas en la sesión de PowerShell o entorno de ejecución del IDE.
   - *Solución:* Ejecutar los comandos `$env:...` de la sección 3 en la misma sesión desde donde se invoca el proceso Java.

2. **Formato de URL JDBC inválido:**
   - *Mensaje:* `El valor de TPI_DB_URL ('...') no tiene un formato JDBC de PostgreSQL válido.`
   - *Causa:* La URL no comienza con `jdbc:postgresql://` o está incompleta.
   - *Solución:* Asignar `jdbc:postgresql://localhost:5432/tpi_pa2`.

3. **Servidor PostgreSQL detenido o inaccesible (`SQLState: 08001` / `08006`):**
   - *Mensaje:* `No se pudo conectar con el servidor PostgreSQL en 'jdbc:postgresql://localhost:5432/tpi_pa2'. Verifique que PostgreSQL local esté instalado, en ejecución y aceptando conexiones en el puerto indicado.`
   - *Causa:* El servicio local de PostgreSQL no está instalado, se encuentra detenido o escucha en un puerto distinto a `5432`.
   - *Solución:* Verificar el estado del servicio local en Windows (`Get-Service *postgres*`) y confirmar el puerto.

4. **Credenciales inválidas (`SQLState: 28P01` / `28000`):**
   - *Mensaje:* `Autenticación rechazada por PostgreSQL para el usuario '...'. Verifique el usuario y la contraseña configurados en las variables de entorno.`
   - *Causa:* El usuario o la contraseña provistos en `TPI_DB_USER` / `TPI_DB_PASSWORD` no coinciden con los registrados en PostgreSQL. (Por seguridad, el mensaje nunca expone la contraseña ingresada).

5. **Base de datos inexistente (`SQLState: 3D000`):**
   - *Mensaje:* `La base de datos indicada en 'jdbc:postgresql://localhost:5432/tpi_pa2' no existe en el servidor PostgreSQL local.`
   - *Causa:* El servidor PostgreSQL está activo, pero aún no se ha creado la base de datos `tpi_pa2`.
   - *Solución:* Crear la base de datos local `tpi_pa2` en PostgreSQL antes de ejecutar la comprobación (ver sección 6).

---

## 6. Inicialización del esquema relacional y datos de referencia

Para preparar la base de datos PostgreSQL local para su uso con la aplicación Desktop, deben ejecutarse los scripts SQL provistos en el proyecto Maven.

### 6.1. Ubicación de los scripts
Los scripts se encuentran versionados en el directorio de recursos de Maven:
- **Esquema relacional (DDL):** `src/main/resources/db/schema.sql`
- **Datos iniciales y catálogos (DML):** `src/main/resources/db/seed_data.sql`

### 6.2. Creación manual de la base de datos local
Antes de inicializar las tablas, la base de datos `tpi_pa2` debe ser creada manualmente en el servidor PostgreSQL local.

Desde PowerShell, una vez que PostgreSQL y la herramienta `psql` estén disponibles en el entorno:

```powershell
# Opción 1: Mediante la utilidad createdb
createdb -h localhost -p 5432 -U <tu_usuario_local> tpi_pa2

# Opción 2: Directamente con psql
psql -h localhost -p 5432 -U <tu_usuario_local> -d postgres -c "CREATE DATABASE tpi_pa2 WITH ENCODING 'UTF8';"
```

### 6.3. Orden estricto de ejecución
Los scripts deben ejecutarse en forma secuencial y ordenada:
1. **Primero:** `schema.sql` (crea las 17 tablas, restricciones de integridad referencial, checks e índices).
2. **Segundo:** `seed_data.sql` (inserta los catálogos y parámetros de referencia iniciales indispensables).

> **Aviso de seguridad:** En Release 1, la clase `ConexionBD.java` **no ejecuta scripts SQL automáticamente**. La inicialización del esquema es una operación manual y controlada del administrador del entorno.

### 6.4. Ejecución de los scripts con psql (PowerShell)
Ubicarse en el directorio raíz del proyecto Java (`03_Trabajo\TPI-ProgAvII`) y ejecutar:

```powershell
# 1. Definir credenciales de sesión local para psql (ilustrativo)
$env:PGPASSWORD = "<tu_clave_local>"

# 2. Ejecutar el esquema relacional
psql -h localhost -p 5432 -U <tu_usuario_local> -d tpi_pa2 -f "src/main/resources/db/schema.sql"

# 3. Ejecutar la carga de datos iniciales
psql -h localhost -p 5432 -U <tu_usuario_local> -d tpi_pa2 -f "src/main/resources/db/seed_data.sql"

# 4. Limpiar la variable temporal de contraseña
Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
```

### 6.5. Relación con las variables de entorno de ConexionBD.java
Los parámetros utilizados para ejecutar `psql` coinciden conceptualmente con las variables de entorno leídas por `ConexionBD`:
- Host (`localhost`), puerto (`5432`) y base (`tpi_pa2`) forman la URL `TPI_DB_URL` (`jdbc:postgresql://localhost:5432/tpi_pa2`).
- Usuario utilizado en `-U` corresponde a `TPI_DB_USER`.
- Contraseña utilizada en `PGPASSWORD` corresponde a `TPI_DB_PASSWORD`.

Una vez ejecutados ambos scripts, la comprobación con `mvn compile exec:java "-Dexec.mainClass=com.gestionamateur.tpi.progav.dao.ConexionBD"` validará la conectividad contra la base de datos ya inicializada.

