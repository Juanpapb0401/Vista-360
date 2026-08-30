# Supuestos declarados, Vista 360° del Estudiante

**Documento transversal.** El caso es deliberadamente abierto. Este documento reúne los supuestos que tomé para poder decidir, con su justificación y el impacto que tendría equivocarme. Las Partes 1 a 4 se apoyan en estos supuestos y los referencian por su identificador.

**Cómo leerlo.** Cada supuesto indica: qué asumo, por qué es razonable, y qué cambiaría del diseño si resultara falso. Los marcados como **[Riesgo alto]** son aquellos cuya invalidación obligaría a rediseñar algo estructural; son los que preguntaría primero en una sesión real con la Universidad.

---

## A. Alcance y usuarios

### SUP-01 · Vista 360° es sistema de registro únicamente para el dominio de acompañamiento
Vista 360° es *fuente de verdad* de los reportes de acompañamiento, alertas, solicitudes y de la asignación estudiante–profesional. Para todo lo demás (datos personales, académicos, financieros, actividad en campus virtual) es **consumidor**, nunca dueño.

*Justificación:* el enunciado es explícito, la plataforma "no reemplaza a ningún sistema existente" y el ERP es "fuente de verdad". Duplicar propiedad de datos maestros crearía dos verdades en conflicto.

*Si fuera falso:* si la Universidad quisiera migrar algún dominio hacia Vista 360°, cambiaría de una arquitectura de composición a una de migración progresiva, con estrategia de convivencia y corte.

### SUP-02 · La asignación estudiante–profesional de acompañamiento no existe hoy y la gestiona Vista 360° **[Riesgo alto]**
No hay en el ecosistema un registro de qué profesional acompaña a qué estudiante. Vista 360° lo modela y lo persiste.

*Justificación:* el enunciado dice que los registros de acompañamiento "son nuevos: no existen hoy en ningún sistema". La asignación pertenece al mismo dominio funcional. Además, sin ella no es posible resolver la autorización de la Parte 3.1, que exige que el profesional vea "solo a los estudiantes que tiene asignados".

*Si fuera falso:* si la asignación viviera en el ERP o en un directorio institucional, Vista 360° la consumiría y la mantendría en caché local para no depender del ERP en cada decisión de autorización. El modelo de autorización no cambia; cambia la fuente.

### SUP-03 · Existen cuatro perfiles de usuario
Estudiante, profesional de acompañamiento, director de centro de apoyo y administrador de la plataforma.

*Justificación:* los dos primeros están en el enunciado. El director de centro de apoyo aparece en la Parte 4, Escenario A, lo que implica una jerarquía por encima del profesional: un director ve los estudiantes de los profesionales de su centro. El administrador es necesario para gestionar asignaciones y parametrización.

*Si fuera falso:* menos perfiles simplifica el modelo de autorización; más perfiles se acomodan sin rediseño si la autorización se resuelve por políticas y no por condicionales incrustados en el código.

### SUP-04 · La asignación es temporal y auditable
Un estudiante tiene 0..1 profesional asignado vigente en un momento dado; un profesional atiende N estudiantes. Las asignaciones tienen fecha de inicio y fin, y se conserva el histórico.

*Justificación:* el personal rota. Si un profesional dejó de atender a un estudiante en marzo, no debería seguir viendo su información en septiembre, pero sí debe quedar registro de que legítimamente la consultó en su momento, lo cual es indispensable para responder el Escenario B de la Parte 4.

*Si fuera falso:* si se permitieran asignaciones múltiples simultáneas (por ejemplo, un psicólogo y un tutor académico), el modelo pasa a una relación N:M con un atributo de rol de acompañamiento. Es un cambio menor y el diseño lo contempla.

### SUP-05 · Vista 360° es de consulta y registro, no transaccional sobre sistemas externos
No se matricula, no se pagan facturas ni se modifican notas desde la plataforma. Las únicas escrituras son sobre su propio dominio de acompañamiento.

*Justificación:* el enunciado describe consulta ("ve, en un solo lugar") y registro de acompañamiento. Ninguna escritura hacia el ERP o el LMS aparece en los objetivos.

*Si fuera falso:* escribir hacia el ERP introduciría necesidad de transaccionalidad distribuida, compensaciones e idempotencia sobre sistemas de terceros. Es un salto de complejidad grande y debe declararse explícitamente como alcance aparte.

### SUP-05b · La solución se despliega en dos piezas, no en un monolito único ni en varios microservicios
Vista 360° se compone de dos aplicaciones independientes: el **Servicio Académico** (matrículas y notas, exigido como pieza separada por la Parte 2 del enunciado, con su propio repositorio) y **Vista 360° Core**, un monolito modular que agrupa dos módulos internos con fronteras estrictas de código y de esquema de datos, el módulo de Acompañamiento y el módulo de Agregación (que compone la vista consolidada consultando al ERP, al LMS y al propio Servicio Académico).

*Justificación:* la escala real, unos 10.000 estudiantes y un equipo de acompañamiento pequeño, no justifica el costo operativo de un sistema distribuido de varias piezas, con pipelines, autenticación servicio a servicio y observabilidad distribuida propias de cada una. Al mismo tiempo, Acompañamiento y Agregación son dominios distintos, con dueños de datos distintos y reglas de autorización distintas, así que mezclarlos sin fronteras de código sería igual de riesgoso. La separación en dos piezas es el punto donde el costo de operar más de una aplicación se paga solo donde el enunciado lo exige de forma explícita.

*Si fuera falso:* si el equipo de acompañamiento creciera de forma sustancial, si apareciera un tercer dominio con reglas propias, o si el volumen de estudiantes creciera en un orden de magnitud, valdría la pena separar Acompañamiento de Agregación en dos servicios propios. Las fronteras de código ya estarían definidas desde el diseño modular, así que ese cambio sería una extracción de módulo a servicio, no un rediseño desde cero.

---

## B. Ecosistema e integración

### SUP-06 · La plataforma de identidad soporta OpenID Connect sobre OAuth 2.0, y podría corresponder a un microservicio propio (Saamfi) en lugar de un IdP comercial
Se asume un proveedor de identidad capaz de emitir tokens de identidad y de acceso, con soporte para Authorization Code + PKCE y para client credentials. Como referencia de contexto: se tiene conocimiento no confirmado de que la Universidad opera on-premise un microservicio propio de autenticación (Saamfi) que valida credenciales y emite un JWT para autorizar el acceso a distintos recursos, es decir, no necesariamente un IdP comercial tipo Keycloak o Azure Entra ID, sino un componente construido a la medida.

*Justificación:* el enunciado del caso habla en abstracto de "plataforma de identidad... con estándares abiertos de identidad", sin nombrar un producto. Esto es intencional: el ejercicio evalúa el razonamiento arquitectónico, no el acierto de adivinar el nombre de un sistema interno real. Por eso el diseño se apoya en el **contrato** (un JWT firmado, estándar, verificable) y no en la implementación concreta detrás de él. Que ese emisor sea Keycloak, Azure Entra ID o un microservicio propio como Saamfi es, para efectos de este diseño, un detalle de implementación intercambiable, lo que importa es que exponga las garantías de un emisor de tokens estándar (firma verificable, expiración, claims de identidad y de rol).

*Si fuera falso:* si Saamfi (o el emisor real que use la Universidad) no siguiera el estándar JWT/OIDC de forma completa, por ejemplo, si el token no fuera un JWT verificable de forma independiente, o si la validación exigiera una llamada síncrona al propio Saamfi en cada petición en lugar de validación local de firma, el diseño de autorización seguiría siendo válido en su forma (basado en claims dentro del token), pero cambiaría el patrón de validación: de "verificar firma localmente con la clave pública del emisor" a "validar el token contra Saamfi en cada llamada", lo que añade una dependencia síncrona adicional y debe considerarse en la disponibilidad de todo el sistema. Este es el punto de mayor incertidumbre de este supuesto, y el primero que verificaría con el equipo de TI de la Universidad antes de construir sobre él.

### SUP-07 · El ERP expone APIs parciales y acceso de lectura a la base de datos
Existen algunas APIs para datos maestros y académicos, pero no cubren todo lo que Vista 360° necesita. Para los vacíos hay acceso de solo lectura a la base de datos, preferiblemente sobre una réplica.

*Justificación:* es literal del enunciado, "Algunas APIs y acceso a la BD".

*Si fuera falso:* si las APIs cubrieran todo, se elimina el adaptador de base de datos y el diseño se simplifica. Si no hubiera ninguna API, todo el acceso pasa por captura de cambios y réplica, lo que refuerza la necesidad de una capa de materialización.

### SUP-08 · Vista 360° nunca accede directamente a la base de datos del ERP **[Riesgo alto]**
Cuando haya que leer de la base del ERP, ese acceso se encapsula detrás de un adaptador dedicado o de la plataforma de integración, que expone un contrato estable.

*Justificación:* acoplar Vista 360° al esquema físico del ERP significa que cualquier actualización del ERP puede romper la plataforma en producción, sin aviso. Además, el acceso directo a la base de un sistema on-premise desde una aplicación nueva amplía la superficie de exposición de datos sensibles y hace imposible auditar el acceso por dominio funcional. El costo de un adaptador se paga solo la primera vez que el proveedor del ERP publica un parche.

*Si fuera falso:* si por restricciones organizacionales se impusiera el acceso directo, exigiría al menos una vista de base de datos como contrato, usuario de solo lectura con permisos mínimos y un acuerdo formal de cambio de esquema con el equipo del ERP.

### SUP-09 · La plataforma de integración ofrece mediación síncrona y mensajería asíncrona
Se asume que puede exponer y proteger APIs (patrón puerta de enlace) y que dispone de un canal de eventos con publicación y suscripción, con entrega garantizada al menos una vez.

*Justificación:* el enunciado le atribuye "mediación, orquestación y mensajería". Es exactamente el reparto de responsabilidades de una plataforma de integración empresarial moderna.

*Si fuera falso:* si solo hubiera mediación síncrona, habría que incorporar un intermediario de mensajería propio para resolver el Escenario B de la Parte 3, con el costo operativo que implica.

### SUP-10 · Los cambios relevantes del ERP pueden convertirse en eventos
Sea porque el ERP los emite, porque la plataforma de integración los detecta mediante captura de cambios, o porque un proceso los deriva por consulta periódica.

*Justificación:* la Parte 3, Escenario B, exige "actuar de forma temprana" ante un cambio de condición académica. Sin alguna forma de notificación de cambio, la única alternativa es consulta periódica intensiva, que no es temprana ni sostenible.

*Si fuera falso:* si el ERP fuera completamente opaco a cambios, se degradaría a consulta programada con la latencia y el costo que eso implica, y habría que negociar la frecuencia aceptable con el negocio.

### SUP-11 · El LMS es un servicio en la nube con límites de tasa y latencia variable
Expone API REST autenticada, con cuotas de consumo y tiempos de respuesta menos predecibles que los sistemas internos.

*Justificación:* es característico de un LMS comercial en modalidad servicio. Asumir lo contrario sería optimista y llevaría a un diseño frágil.

*Si fuera falso:* si no hubiera límites, se podría consultar en línea con más libertad; el diseño propuesto sigue siendo válido, simplemente más conservador de lo necesario.

### SUP-12 · Vista 360° alimenta el data warehouse; no escribe en su modelo dimensional
La plataforma publica sus datos y eventos hacia la capa de ingesta del data warehouse. La transformación al modelo analítico es responsabilidad del equipo de datos.

*Justificación:* el enunciado describe el data warehouse como "repositorio analítico del ecosistema", con modelos y tableros construidos encima. Que una aplicación transaccional escriba directamente en el modelo dimensional rompe la separación entre lo operacional y lo analítico y convierte al equipo de datos en rehén de los cambios de la aplicación.

*Si fuera falso:* si la Universidad exigiera escritura directa, se acordaría un contrato de datos formal y versionado como frontera entre ambos equipos.

---

## C. Datos y propiedad

### SUP-13 · El identificador canónico del estudiante es el código institucional del ERP **[Riesgo alto]**
Todos los sistemas correlacionan al estudiante por ese código. El token emitido por la plataforma de identidad debe permitir resolver ese código, sea porque lo incluye o porque existe una tabla de correspondencia.

*Justificación:* el ERP es la fuente de verdad de los datos maestros; su identificador es el que tiene sentido como clave de correlación. Sin un identificador canónico, integrar cinco sistemas se vuelve un problema de conciliación de identidades.

*Si fuera falso:* si cada sistema usara su propio identificador sin correspondencia establecida, haría falta un servicio de resolución de identidades como componente de primera clase, y sería lo primero a construir.

### SUP-14 · Los datos financieros se consultan en vivo y no se almacenan en caché
El estado financiero se obtiene del ERP en el momento de la consulta.

*Justificación:* mostrar un saldo desactualizado tiene consecuencias reales, un estudiante que cree estar a paz y salvo cuando no lo está, o al revés. La corrección pesa más que la latencia en este dato puntual. Se detalla en la Parte 3, Escenario A.

*Si fuera falso:* si el negocio aceptara una ventana de desactualización explícita, se podría almacenar en caché con marca de tiempo visible al usuario.

### SUP-15 · Se admite caché de corta duración para datos de baja volatilidad
Datos personales, programa académico e histórico de semestres cerrados toleran una ventana de desactualización de minutos a horas.

*Justificación:* estos datos cambian rara vez y su desactualización no produce decisiones erróneas. Consultarlos en vivo en cada carga de pantalla castigaría al ERP sin beneficio.

*Si fuera falso:* si algún dato de este grupo resultara volátil, se reclasifica individualmente. La política de caché se define por dato, no en bloque.

### SUP-16 · Lo que Vista 360° materialice de otros sistemas es copia auxiliar, no maestra
Cualquier dato replicado se marca con su origen y con la marca de tiempo de la última sincronización, y nunca se presenta como autoritativo frente al sistema fuente.

*Justificación:* protege la coherencia con SUP-01 y permite responder con honestidad cuándo se obtuvo cada dato, algo necesario tanto para el usuario como para la auditoría de la Parte 4.

---

## D. Seguridad y cumplimiento

### SUP-17 · Aplica el régimen colombiano de protección de datos personales
La solución maneja datos personales y algunos sensibles, sujetos a la Ley 1581 de 2012 y su decreto reglamentario. Se asume que la Universidad tiene política de tratamiento de datos vigente y que Vista 360° debe cumplirla, no redefinirla.

*Justificación:* es una universidad colombiana manejando información académica, financiera y de acompañamiento. Los reportes de acompañamiento pueden contener información sobre situaciones personales delicadas, lo que eleva el nivel de cuidado exigible.

*Si fuera falso:* en otra jurisdicción cambian los plazos y las obligaciones formales, pero no los controles técnicos: mínimo privilegio, trazabilidad y cifrado siguen siendo los mismos.

### SUP-18 · Todo el tráfico va cifrado en tránsito y los datos sensibles en reposo
Cifrado obligatorio en todas las comunicaciones, incluidas las internas entre servicios.

*Justificación:* es línea base no negociable para información académica y financiera nominada. Confiar en que la red interna es segura es un supuesto que ya no se sostiene.

### SUP-19 · Se requiere auditoría íntegra de accesos y cambios
Toda consulta a información de un estudiante y toda modificación de registros de acompañamiento se registran de forma que no puedan alterarse sin dejar rastro.

*Justificación:* la Parte 4, Escenario B, plantea exactamente este escenario, un estudiante que reclama y una institución que debe responder "con certeza". Sin auditoría diseñada desde el principio, esa certeza no existe; se reconstruye a mano, tarde y sin garantías.

---

## E. Volumetría y requisitos no funcionales

### SUP-20 · Escala mediana, con estacionalidad marcada
Universidad Icesi, aproximadamente 10.000 estudiantes activos (pregrado y posgrado) y algunas decenas de profesionales de acompañamiento. La carga se concentra en inicio de semestre, publicación de notas y fechas de corte de pagos.

*Justificación:* cifra aproximada según fuentes públicas de la Universidad Icesi. A esta escala no se justifica operar un sistema distribuido de varias piezas, lo cual respalda directamente la decisión declarada en SUP-05b de trabajar con solo dos aplicaciones desplegables, en vez de fragmentar en varios microservicios. Añadir escalamiento horizontal agresivo o más piezas distribuidas sería sobreingeniería no justificada por el volumen real.

*Si fuera falso:* un orden de magnitud más grande obligaría a revisar las estrategias de caché y de materialización, no la arquitectura de fondo.

### SUP-21 · La caída de un sistema fuente no debe tumbar la vista completa **[Riesgo alto]**
Si el ERP o el LMS no responden, Vista 360° muestra la información disponible e indica con claridad qué sección no pudo cargar y por qué.

*Justificación:* una vista consolidada que depende de cinco sistemas tiene, por construcción, peor disponibilidad que cualquiera de ellos si se diseña de forma ingenua. La degradación parcial es lo que hace viable el producto. Además es la contraparte de diseño del Escenario A de la Parte 4: para saber que "no carga" hay que haber diseñado qué significa "no cargó".

### SUP-22 · Objetivos de servicio declarados
Disponibilidad del 99,5% en horario académico y latencia del percentil 95 por debajo de 2 segundos para la vista consolidada.

*Justificación:* son objetivos alcanzables sin sobreingeniería y suficientes para el caso de uso. Lo relevante no es la cifra exacta sino que exista una cifra: sin objetivo declarado no hay forma de saber si un incidente es un incidente.

---

## F. Operación

### SUP-23 · Se dispone de observabilidad centralizada, o forma parte de la propuesta
Registro de eventos centralizado, métricas y trazas distribuidas con identificador de correlación que atraviese todos los componentes.

*Justificación:* el Escenario A de la Parte 4 describe un fallo intermitente y difícil de reproducir. Ese tipo de incidente solo se resuelve con evidencia; sin trazabilidad, se resuelve con suerte. Si la Universidad no cuenta con la plataforma, se incluye en el alcance del proyecto.

### SUP-24 · Despliegue en contenedores con automatización de entrega
Se asume capacidad de desplegar contenedores y de mantener entornos separados de desarrollo, pruebas y producción.

*Justificación:* es la línea base de la industria y condición para poder desplegar correcciones con rapidez, lo cual forma parte de la respuesta operativa de la Parte 4.

---

## Resumen de supuestos de riesgo alto

Si tuviera una sola reunión con la Universidad antes de construir, preguntaría por estos cinco:

| ID | Supuesto | Por qué es crítico |
|---|---|---|
| SUP-02 | La asignación estudiante–profesional no existe y la gestiona Vista 360° | De él depende todo el modelo de autorización |
| SUP-08 | No hay acceso directo a la base de datos del ERP | Define el acoplamiento estructural de la solución |
| SUP-13 | Existe un identificador canónico del estudiante | Sin él, integrar cinco sistemas es un problema distinto |
| SUP-21 | Se acepta degradación parcial ante caída de un sistema fuente | Determina la viabilidad de una vista consolidada |
| SUP-06 | La identidad institucional habla OpenID Connect | Condiciona el mecanismo de autenticación de extremo a extremo |

---

## Nota sobre el uso de herramientas de inteligencia artificial

*(Pendiente de completar al cierre del ejercicio, según lo solicitado en las instrucciones generales: qué herramienta se usó, en qué partes y con qué propósito.)*
