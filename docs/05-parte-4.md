# Parte 4 · Operación y calidad

Este documento se apoya en `00-supuestos.md` (referenciado como SUP-XX), en `01-arquitectura.md` y en `04-parte-3.md`. Los dos escenarios de esta parte no son hipotéticos abstractos: son consecuencia directa de decisiones ya tomadas en las partes anteriores, y aquí se explica qué de esas decisiones ya cubre el problema y qué haría falta construir encima.

## Escenario A: información académica que no carga de forma intermitente

Director de un centro de apoyo reporta que, ocasionalmente, la información académica de ciertos estudiantes no carga. El comportamiento es intermitente y difícil de reproducir.

### Cómo se afrontaría el incidente

Un fallo intermitente y difícil de reproducir casi nunca se resuelve intentando reproducirlo a mano; se resuelve con evidencia que ya existía cuando el fallo ocurrió. Por eso el primer paso no es "intentar que vuelva a pasar", es **acotar el problema con lo que ya se registró**:

1. **Delimitar el alcance.** ¿Qué estudiantes, qué momentos, qué director? Vista 360° compone información académica de dos fuentes distintas (ver el mapa de datos en `01-arquitectura.md`): la proyección propia del Servicio Académico (lectura local, rara vez falla) y datos que se leen en vivo del ERP a través de la plataforma de integración (con salto de red, sujeto a la disponibilidad del ERP on-premise). El primer diagnóstico es identificar cuál de las dos rutas está fallando, porque cambia por completo dónde se busca después.
2. **Correlacionar por identificador de solicitud.** Con un identificador de correlación generado en el Frontend y propagado en cada salto (Frontend → Integración → Vista 360° Core / Servicio Académico → ERP), se puede reconstruir el recorrido exacto de las peticiones que fallaron, filtrando los registros por el rango de tiempo y los estudiantes que reportó el director, en vez de revisar logs sin ningún hilo conductor.
3. **Cruzar con métricas de la dependencia sospechosa.** Si el patrón apunta al ERP, se revisa la tasa de error y la latencia de esa integración en el mismo rango de tiempo: picos de latencia, agotamiento del pool de conexiones a la réplica de solo lectura, o una ventana de mantenimiento del ERP que coincide con los reportes.
4. **Reproducir con el patrón identificado**, no al azar: mismo tipo de estudiante, mismo momento del día, misma carga concurrente, en un ambiente de pruebas si es posible.
5. **Corregir y agregar una prueba de regresión** o un monitor sintético que ejecute periódicamente la misma consulta que falló, para detectar la próxima recurrencia antes de que un director tenga que reportarla.

### Qué se habría necesitado prever desde el diseño

Esta es la parte que de verdad separa un incidente resuelto en una hora de uno que toma una semana, y es enteramente una decisión de diseño, no de reacción:

- **Identificador de correlación de punta a punta** (SUP-23). Sin él, cada uno de los cinco sistemas del ecosistema tiene su propio log, sin forma de unirlos. Con él, una sola búsqueda reconstruye el recorrido completo de una petición específica.
- **Registro estructurado en cada salto**, no texto libre: cada llamada saliente (a qué sistema, cuánto tardó, con qué resultado) queda como un evento consultable, no como una línea de texto que hay que interpretar.
- **Presupuestos de tiempo de espera explícitos por cada llamada externa**, ajustados a lo que cada sistema puede ofrecer razonablemente, en vez de dejar el valor por defecto del framework. Sin esto, es imposible distinguir "el ERP está lento" de "el ERP no respondió".
- **Cortacircuitos con reintentos acotados hacia el ERP y el LMS**, con el estado del cortacircuito expuesto como métrica. Esto evita que una falla intermitente se agrave en cascada, y convierte un problema silencioso en una señal operativa visible ("la integración con el ERP lleva 4 minutos en estado abierto") antes de que alguien tenga que reportarlo.
- **Métricas de tasa de error y latencia por dependencia externa, con alertas.** El objetivo es que el equipo se entere del problema antes que los directores de los centros de apoyo, no después.
- **Degradación parcial explícita** (SUP-21, ya implementada en el diseño del Escenario A de la Parte 3, ver `04-parte-3.md`): cuando una sección no puede cargar, la vista lo dice claramente en vez de fallar en silencio o mostrar una pantalla en blanco. Esto no solo mejora la experiencia, cambia la naturaleza del reporte que llega a soporte: "la sección financiera dice que no pudo cargar" es un reporte accionable; una pantalla rota o vacía no lo es.
- **Verificaciones de salud sintéticas** contra el adaptador del ERP, que prueben la ruta completa de forma periódica y no solo esperen a que un usuario real la dispare.

## Escenario B: reclamo de acceso indebido a información de un estudiante

Un estudiante sospecha que su información fue consultada o alterada por alguien que no debía hacerlo, y la institución necesita responder con certeza, no con una suposición razonable.

### Qué se habría previsto desde el diseño

Responder "con certeza" solo es posible si el sistema registró, desde antes de que existiera el reclamo, cada acceso a la información de ese estudiante. No es algo que se pueda reconstruir después con buena voluntad; o el registro existía, o la institución solo puede especular. Esto ya está declarado como SUP-19 en `00-supuestos.md`; aquí se detalla en qué consistiría:

- **Un registro de auditoría de accesos, separado del dato transaccional.** Cada lectura y cada escritura sobre información de un estudiante queda registrada con: quién la hizo (el `sub` del token, y si fue un token de servicio, el encabezado `X-Actuando-En-Nombre-De` que preserva en representación de qué profesional se hizo la consulta, ver `04-parte-3.md`), qué recurso se consultó o modificó, cuándo (con reloj sincronizado), y el resultado (autorizado o denegado).
- **Los intentos denegados se registran igual que los exitosos.** Un `403` no es un no-evento: que alguien intentó acceder sin autorización es evidencia tan relevante como un acceso exitoso indebido, especialmente para descartar (o confirmar) un patrón de intentos repetidos.
- **El registro es de solo escritura para la aplicación.** El mismo servicio que atiende las consultas no debe poder alterar ni borrar su propio historial de auditoría; el rol de base de datos que usa la aplicación tendría permiso de `INSERT` sobre esa tabla, pero no de `UPDATE` ni `DELETE`. A la escala de este caso no hace falta nada más sofisticado (como encadenamiento criptográfico de cada registro); si el nivel de riesgo institucional lo exigiera más adelante, es una capa que se añade sin rediseñar el mecanismo.
- **La validez temporal de las asignaciones importa tanto como el registro de acceso.** Ya se declaró en SUP-04 que la asignación estudiante-profesional se conserva con histórico, no solo el estado actual. Para responder "¿tenía este profesional autorización para ver a este estudiante en ese momento?", hace falta cruzar el registro de auditoría con la asignación vigente *en esa fecha*, no con la asignación de hoy.
- **Cuando el acceso llegó vía un token de servicio, la certeza sobre la persona exige cruzar dos registros, no uno.** El registro de auditoría del Servicio Académico, en esos casos, solo prueba que Vista 360° Core *afirmó* actuar en nombre de un profesional puntual (a través del encabezado `X-Actuando-En-Nombre-De`, ver `04-parte-3.md`); ese encabezado es autoafirmado, no autenticado por el Servicio Académico. Responder "con certeza" quién fue la persona exige cruzar ese registro con el propio registro de acceso de Vista 360° Core, donde sí se autenticó al profesional con su token de usuario. Es la misma correlación del punto anterior, aplicada a la identidad en vez de al permiso.
- **Política de retención definida explícitamente.** Cuánto tiempo se conserva el registro de auditoría no es una decisión técnica, es una decisión institucional (legal, de cumplimiento); el diseño solo garantiza que el dato existe y es consultable mientras la política vigente lo exija.

### Cómo se respondería al reclamo, una vez ocurre

Con el diseño anterior en su lugar, responder al reclamo es una consulta, no una investigación desde cero: se filtra el registro de auditoría por el estudiante y el rango de fechas del reclamo, se identifica cada acceso (quién, cuándo, autorizado o no), y para los accesos de profesionales se valida contra la asignación vigente en ese momento. El resultado es un reporte verificable, no una reconstrucción de memoria de nadie.

### Qué falta por construir

El Servicio Académico (Parte 2) hoy registra errores con su traza en el log de la aplicación, pero **no tiene todavía** el registro de auditoría estructurado y de solo escritura descrito arriba; está diseñado, no construido. Es, junto con el consumidor real de eventos de sincronización, uno de los puntos explícitamente declarados fuera de alcance en el README del servicio.