-- Idempotencia de la sincronización de evaluaciones.
--
-- La plataforma de integración garantiza entrega "al menos una vez" (SUP-09 en
-- docs/00-supuestos.md), así que reprocesar el mismo evento no es un caso raro:
-- es el comportamiento esperado ante cualquier reintento. Sin una clave de
-- negocio que identifique la evaluación en su sistema de origen, cada reintento
-- insertaba una fila nueva y distorsionaba nota_actual.
--
-- La columna admite NULL para no invalidar las evaluaciones ya cargadas por la
-- migración de datos de prueba; la restricción de unicidad ignora los NULL tanto
-- en H2 como en PostgreSQL, así que solo aplica a las que sí llegan por evento.

ALTER TABLE evaluacion ADD COLUMN id_evaluacion_origen VARCHAR(64) NULL;

CREATE UNIQUE INDEX uq_evaluacion_origen ON evaluacion (id_evaluacion_origen);
