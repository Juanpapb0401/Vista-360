# Parte 1 · Diseño de la arquitectura

Este documento presenta el diagrama de arquitectura de Vista 360° del Estudiante y las decisiones que lo sostienen. Se apoya en la hoja de supuestos (`00-supuestos.md`), referenciada aquí por identificador (SUP-XX) cada vez que una decisión depende de uno declarado allí.

La arquitectura se compone de dos aplicaciones nuevas y desplegables de forma independiente, tal como se fijó en SUP-05b: el **Servicio Académico** (microservicio, exigido como pieza propia por la Parte 2) y **Vista 360° Core** (monolito modular, con los módulos internos Acompañamiento y Agregación). El resto del ecosistema ya existe y no se modifica.

## Diagrama general de componentes

Se presenta en dos vistas separadas, en vez de una sola con todo mezclado, porque juntar el flujo síncrono y el asíncrono en un mismo diagrama satura las etiquetas de las flechas y dificulta la lectura. Cada vista cuenta una sola historia.

### Vista 1: flujo síncrono (consultas del usuario)

![Flujo síncrono de Vista 360°](img/01-flujo-sincrono.png)

<details>
<summary>Código fuente del diagrama</summary>

```mermaid
flowchart TD
    subgraph USR["Usuarios"]
        EST["Estudiante"]
        ACOMP["Equipo de acompañamiento"]
    end

    subgraph V360["Vista 360° del Estudiante (nuevo)"]
        FE["Frontend Vista 360°"]
        CORE["Vista 360° Core
(módulos: Acompañamiento y Agregación)"]
        ACAD["Servicio Académico
(BD propia, proyección de matrículas y notas)"]
    end

    subgraph ECO["Ecosistema existente"]
        ID["Plataforma de Identidad
(OIDC / JWT)"]
        INT["Plataforma de Integración
(mediación + mensajería)"]
        ERP["ERP Institucional
(fuente de verdad)"]
        LMS["LMS
(cloud, API)"]
    end

    EST --> FE
    ACOMP --> FE
    FE -.->|login OIDC, redirección| ID
    FE -->|REST, Bearer JWT| INT
    INT -->|REST| CORE
    INT -->|REST| ACAD
    INT -->|REST, solo lectura| ERP
    INT -->|REST| LMS
```
</details>

### Vista 2: flujo asíncrono (eventos y sincronización de fondo)

![Flujo asíncrono de Vista 360°](img/02-flujo-asincrono.png)

<details>
<summary>Código fuente del diagrama</summary>

```mermaid
flowchart TD
    subgraph V360["Vista 360° del Estudiante (nuevo)"]
        CORE["Vista 360° Core
(módulos: Acompañamiento y Agregación)"]
        ACAD["Servicio Académico
(BD propia, proyección de matrículas y notas)"]
    end

    subgraph ECO["Ecosistema existente"]
        INT["Plataforma de Integración
(mediación + mensajería)"]
        ERP["ERP Institucional
(fuente de verdad)"]
        DWH["Data Warehouse"]
    end

    ERP -.->|cambio condición académica| INT
    INT -.->|evento| CORE
    CORE -.->|evento de acompañamiento| INT
    ACAD -.->|sincroniza proyección| INT
    INT -.->|evento / batch de ingesta| DWH
```
</details>

**Cómo leer ambas vistas:** línea sólida es comunicación síncrona (pido y espero respuesta); línea punteada es comunicación asíncrona (evento o sincronización de fondo). En la Vista 1, todo el tráfico síncrono hacia sistemas externos pasa por la Plataforma de Integración como puerta de enlace única, en vez de que cada aplicación de Vista 360° hable directo con el ERP o el LMS. Esa decisión centraliza en un solo punto la protección de esas APIs y contiene el impacto si el ERP cambia un endpoint. La Vista 2 muestra por separado el otro tipo de tráfico, el que nadie está esperando en pantalla, el ERP avisa que algo cambió, y Vista 360° Core y el Servicio Académico se mantienen al día o publican sus propios cambios sin bloquear a ningún usuario.

Un detalle que el diagrama no muestra por claridad: tanto Vista 360° Core como el Servicio Académico validan la firma del JWT de forma local, usando la llave pública que publica la Plataforma de Identidad, sin hacer una llamada de red por cada petición (ver SUP-06). Se detalla en la Parte 3.1.

## De dónde sale cada dato, y por qué

| Dato que necesita Vista 360° | Origen | Por qué se resolvió así |
|---|---|---|
| Identidad, autenticación (usuario, rol) | Plataforma de identidad (JWT) | Estándar abierto ya disponible (SUP-06), no se reinventa autenticación. |
| Datos personales del estudiante | ERP, vía API si existe, si no, adaptador de solo lectura sobre réplica | ERP es fuente de verdad (SUP-01), nunca acceso directo a su BD productiva (SUP-08). |
| Estado financiero (saldo, paz y salvo) | ERP, en vivo, sin caché | Dato sensible a estar desactualizado (SUP-14), ver Escenario A de la Parte 3. |
| Matrículas y notas actuales | Servicio Académico (propio), sincronizado desde el ERP | Exigido como pieza propia por la Parte 2, se materializa para no depender del ERP en cada lectura (SUP-07). |
| Actividad de campus virtual | LMS, vía su API, con caché corta | Fuente única, ya expuesta por API (SUP-11), tolera caché de minutos (SUP-15). |
| Reportes de acompañamiento, alertas, solicitudes | Módulo Acompañamiento (Vista 360° Core) | Dato 100% nuevo, no existe en ningún sistema (SUP-02), Vista 360° es su dueño. |
| Asignación estudiante-profesional | Módulo Acompañamiento (Vista 360° Core) | Mismo dominio que el anterior, necesaria para resolver autorización (SUP-02, SUP-04). |
| Cambio de condición académica (evento) | ERP, vía evento publicado a través de la plataforma de integración | Debe propagarse temprano hacia otros procesos y al DWH (SUP-10), ver Escenario B de la Parte 3. |
| Modelos y tableros analíticos | Data warehouse, alimentado por Vista 360° | Vista 360° es origen, no dueño del modelo dimensional (SUP-12). |

## Cómo se comunican los componentes

| Interacción | Patrón | Por qué |
|---|---|---|
| Frontend a Vista 360° Core / Servicio Académico | Síncrono, REST sobre HTTPS, vía la plataforma de integración | El usuario espera respuesta en pantalla, no hay nada que ganar con asíncrono aquí. |
| Vista 360° Core (Agregación) a ERP, LMS | Síncrono, vía plataforma de integración | Datos que se piden bajo demanda para armar la vista consolidada. |
| Vista 360° Core (Agregación) a ERP, dato financiero | Síncrono, en vivo, sin caché | SUP-14, la corrección pesa más que la latencia en este dato puntual. |
| Servicio Académico a ERP | Asíncrono, eventos vía plataforma de integración, o consulta periódica si el ERP no emite eventos | Es sincronización de una proyección propia, no una respuesta esperada en pantalla, evita golpear al ERP en cada lectura. |
| ERP a Vista 360° Core (Acompañamiento) y a Data warehouse, ante cambio de condición académica | Asíncrono, evento con múltiples suscriptores | Ver Escenario B de la Parte 3 en `04-parte-3.md`, un solo evento, varios interesados, desacopla al ERP de saber quién consume el cambio. |
| Vista 360° Core y Servicio Académico a Data warehouse | Asíncrono, evento o exportación periódica | SUP-12, Vista 360° no escribe directo al modelo dimensional, solo publica. |
| Todos los servicios con la Plataforma de identidad | Validación local de JWT por firma | Cada servicio valida el token que recibe, sin llamada de red por petición (SUP-06). |

## Escenarios de comunicación (Parte 3.2)

Los dos escenarios de comunicación que pide la Parte 3 (consulta del estado financiero en tiempo real, y propagación de un cambio de condición académica) se resuelven con este mismo diseño de comunicación síncrona/asíncrona. Su desarrollo completo, con los diagramas de secuencia y la argumentación, está en [`04-parte-3.md`](04-parte-3.md).

## Decisiones clave de esta parte

1. **Dos piezas desplegables, no una ni varias** (SUP-05b): Servicio Académico como microservicio propio, Vista 360° Core como monolito modular. La escala de Icesi (~10.000 estudiantes, SUP-20) no justifica el costo de operar más piezas distribuidas.
2. **La plataforma de integración es la única puerta de salida hacia el ERP y el LMS.** Ninguna aplicación de Vista 360° les habla directo. Esto contiene el impacto de cualquier cambio en esos sistemas y centraliza la protección de sus APIs.
3. **Vista 360° nunca accede directo a la base de datos del ERP** (SUP-08). Donde no hay API, hay un adaptador dedicado detrás de la plataforma de integración.
4. **Lo síncrono es para lo que el usuario espera ver en pantalla; lo asíncrono es para propagar cambios.** Esta separación es la que resuelve limpio los dos escenarios de comunicación de la Parte 3, desarrollados en [`04-parte-3.md`](04-parte-3.md).
5. **El dato financiero nunca se cachea; el resto sí, con ventanas distintas según su volatilidad** (SUP-14, SUP-15).