# Parte 2 · Especificación del servicio

## Qué hace

Dado el identificador de un estudiante, expone dos niveles de consulta sobre sus materias matriculadas y sus notas en el periodo académico vigente, o en uno específico si se solicita:

1. **Resumen por materia**: la lista de materias matriculadas, cada una con su nota consolidada.
2. **Detalle por materia**: el desglose de evaluaciones (parciales, quices, tareas) que componen la nota de una materia puntual.

Se separan en dos endpoints, en vez de devolver todo en una sola respuesta, porque cada uno pagina de forma independiente y responde a una necesidad de consulta distinta, ver un resumen general, o entrar al detalle de una materia.

## Endpoint 1: resumen de materias

```
GET /api/v1/estudiantes/{codigoEstudiante}/materias
```

### Parámetros de ruta

| Parámetro | Tipo | Descripción |
|---|---|---|
| `codigoEstudiante` | string | Identificador institucional del estudiante (código canónico del ERP, ver SUP-13 en `00-supuestos.md`). |

### Parámetros de consulta

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `periodo` | string, formato `AAAA-N` (ej. `2026-2`) | No | Periodo académico a consultar. Si se omite, se usa el periodo vigente. |
| `page` | entero, base 0 | No | Página a consultar. Por defecto `0`. |
| `size` | entero | No | Tamaño de página. Por defecto `10`, máximo `50`. |

### Encabezados

| Encabezado | Descripción |
|---|---|
| `Authorization` | `Bearer <JWT>`, obligatorio. |
| `X-Actuando-En-Nombre-De` | Opcional. Solo aplica cuando quien llama presenta un token de servicio (`rol=SERVICE`): lleva el código del profesional en cuyo nombre se hace la consulta, para trazabilidad. Ver el diseño completo en `04-parte-3.md`. **Declarado en el diseño de seguridad, no leído todavía por esta implementación** (no hay auditoría de accesos construida aún, ver `05-parte-4.md`); si llega, hoy se ignora sin afectar la respuesta. |

### Respuesta exitosa

`200 OK`

```json
{
  "codigoEstudiante": "A00123456",
  "periodoAcademico": "2026-2",
  "materias": {
    "content": [
      {
        "codigoMateria": "ING1234",
        "nombreMateria": "Estructuras de Datos",
        "grupo": "01",
        "creditos": 3,
        "notaActual": 4.2,
        "estado": "EN_CURSO"
      },
      {
        "codigoMateria": "MAT2201",
        "nombreMateria": "Cálculo Multivariado",
        "grupo": "02",
        "creditos": 4,
        "notaActual": null,
        "estado": "EN_CURSO"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 6,
    "totalPages": 1
  }
}
```

### Notas sobre los campos

- `notaActual` es la nota consolidada de la materia. Se mantiene como un valor ya calculado, no se recalcula en cada consulta: cada vez que se sincroniza una evaluación nueva o modificada (ver Endpoint 2), el componente de sincronización recalcula el promedio ponderado, normalizado por la suma de los porcentajes ya registrados, y lo deja listo para lectura. Es nulo si aún no hay ninguna evaluación registrada. Es el mismo patrón que usan sistemas de gestión de cursos como Moodle o Canvas.
- `estado` refleja la situación de la matrícula en ese periodo (`EN_CURSO`, `APROBADA`, `REPROBADA`, `CANCELADA`).

## Endpoint 2: detalle de evaluaciones de una materia

```
GET /api/v1/estudiantes/{codigoEstudiante}/materias/{codigoMateria}/evaluaciones
```

### Parámetros de ruta

| Parámetro | Tipo | Descripción |
|---|---|---|
| `codigoEstudiante` | string | Igual que en el Endpoint 1. |
| `codigoMateria` | string | Código de la materia, debe corresponder a una materia matriculada por ese estudiante en el periodo consultado. |

### Parámetros de consulta

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `periodo` | string, formato `AAAA-N` | No | Igual que en el Endpoint 1. |
| `page` | entero, base 0 | No | Por defecto `0`. |
| `size` | entero | No | Por defecto `10`, máximo `50`. |

### Respuesta exitosa

`200 OK`

```json
{
  "codigoEstudiante": "A00123456",
  "codigoMateria": "ING1234",
  "nombreMateria": "Estructuras de Datos",
  "periodoAcademico": "2026-2",
  "evaluaciones": {
    "content": [
      {
        "tipo": "PARCIAL",
        "nombre": "Parcial 1",
        "valor": 4.0,
        "porcentaje": 25.0,
        "fecha": "2026-03-15"
      },
      {
        "tipo": "QUIZ",
        "nombre": "Quiz 2 - Recursión",
        "valor": 4.5,
        "porcentaje": 5.0,
        "fecha": "2026-03-20"
      },
      {
        "tipo": "TAREA",
        "nombre": "Taller de árboles binarios",
        "valor": 4.8,
        "porcentaje": 5.0,
        "fecha": "2026-03-22"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 12,
    "totalPages": 2
  }
}
```

### Notas sobre los campos

- `tipo` es una categoría cerrada (`PARCIAL`, `QUIZ`, `TAREA`, `PROYECTO`, `EXAMEN_FINAL`, `OTRO`), para poder agrupar y graficar por tipo de evaluación si se necesita más adelante.
- `porcentaje` es el peso de esa evaluación puntual dentro de la nota final de la materia. La suma de todos los `porcentaje` de una materia, a lo largo del periodo completo, debe ser 100, pero en un corte parcial del semestre puede sumar menos.

## Autenticación y autorización

Aplica igual para ambos endpoints. El servicio valida la firma del JWT de forma local, usando la llave pública publicada por la plataforma de identidad, sin llamada de red por petición (coherente con SUP-06 y con el diseño de la Parte 3.1).

Se reconocen dos tipos de token, y cada uno habilita una regla distinta:

| Tipo de token | Emitido a | Regla de autorización |
|---|---|---|
| Token de usuario (Authorization Code + PKCE), claim `rol=ESTUDIANTE` | El propio estudiante, vía el Frontend | El `codigoEstudiante` de la ruta debe coincidir con el claim `sub` del token. Si no coincide, `403`. |
| Token de servicio (Client Credentials), claim `rol=SERVICE` | Vista 360° Core | Se autoriza la consulta de cualquier `codigoEstudiante`. El Servicio Académico no valida la asignación estudiante-profesional porque no es dueño de ese dato (SUP-02); confía en que Vista 360° Core ya lo validó antes de reenviar la solicitud. |

Cualquier otro caso (token ausente, expirado, con firma inválida, o con un rol no reconocido) responde `401`.

## Respuestas de error

| Código | Cuándo ocurre | Cuerpo |
|---|---|---|
| `400 Bad Request` | El parámetro `periodo` no cumple el formato esperado, o `page`/`size` son inválidos (negativos, o `size` mayor al máximo). | `{ "error": "PARAMETRO_INVALIDO", "mensaje": "..." }` |
| `401 Unauthorized` | Token ausente, expirado, o con firma inválida. | `{ "error": "NO_AUTENTICADO", "mensaje": "..." }` |
| `403 Forbidden` | Token válido, pero sin permiso para consultar el `codigoEstudiante` solicitado. | `{ "error": "NO_AUTORIZADO", "mensaje": "..." }` |
| `404 Not Found` | El `codigoEstudiante` no existe, no tiene matrículas en el periodo consultado, o (Endpoint 2) el `codigoMateria` no corresponde a una materia matriculada por ese estudiante en ese periodo. | `{ "error": "RECURSO_NO_ENCONTRADO", "mensaje": "..." }` |

## Supuestos declarados para esta parte

- **Un estudiante tiene un único periodo académico vigente en un momento dado**, y el servicio lo puede determinar sin que se lo indiquen explícitamente (por fecha de sistema contra un calendario académico almacenado). Si el enunciado esperara que el vigente lo definiera otro sistema, se reemplaza esa lógica por una consulta a esa fuente, sin cambiar el contrato.
- **La nota de una materia es el promedio ponderado de sus evaluaciones**, recalculada de forma automática cada vez que una evaluación se sincroniza, no capturada aparte a mano. Esto evita que la nota consolidada y el detalle de evaluaciones puedan quedar desincronizados entre sí, sin depender de que alguien se acuerde de actualizarla.
- **El Servicio Académico es la proyección propia descrita en la Parte 1**, sincronizada desde el ERP, no una consulta en vivo al ERP en cada petición (ver el mapa de datos en `01-arquitectura.md`).
- **Ambos listados se paginan** (materias, y evaluaciones dentro de una materia), con un tamaño de página máximo de 50 para evitar respuestas sin control de tamaño.