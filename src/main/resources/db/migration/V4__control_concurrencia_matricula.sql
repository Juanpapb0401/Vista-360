-- Control de concurrencia optimista sobre matricula.
--
-- nota_actual se recalcula en cada sincronización de evaluaciones. Dos
-- entregas concurrentes sobre la misma matrícula (normales con entrega
-- "al menos una vez", SUP-09) podían pisarse el valor entre sí sin que
-- nadie lo notara. La columna version, manejada por JPA (@Version), hace
-- que la segunda escritura falle y se reintente en vez de perderse.

ALTER TABLE matricula ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
