-- Create cita_historial table
CREATE TABLE IF NOT EXISTS cita_historial (
    id BIGSERIAL PRIMARY KEY,
    cita_id BIGINT NOT NULL,
    fecha_cita TIMESTAMP,
    tipo VARCHAR(50),
    estado VARCHAR(50),
    observacion VARCHAR(500),
    fecha_modificacion TIMESTAMP NOT NULL,
    usuario_modificacion VARCHAR(100),
    CONSTRAINT fk_cita_historial_cita FOREIGN KEY (cita_id) REFERENCES citas(id) ON DELETE CASCADE
);

CREATE INDEX idx_cita_historial_cita_id ON cita_historial(cita_id);
CREATE INDEX idx_cita_historial_fecha_mod ON cita_historial(fecha_modificacion);
