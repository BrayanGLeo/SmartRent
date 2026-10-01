-- ==========================================
-- SCRIPT DE POBLAMIENTO (SEED DATA) AWS PostgreSQL
-- Proyecto: Pedidos360 / SmartRent
-- ==========================================

-- 1. Poblar la tabla de Categorías (categories)
-- Nota: Asegúrate de que las tablas existan antes de ejecutar este script.
INSERT INTO categories (name, description) VALUES
('Maquinaria Pesada', 'Equipos de gran tamaño para construcción, movimiento de tierras y demolición.'),
('Herramientas Eléctricas', 'Herramientas motorizadas para trabajos de precisión, corte y perforación.'),
('Equipos de Elevación', 'Plataformas, grúas y montacargas para trabajos en altura.'),
('Generadores y Energía', 'Generadores eléctricos, compresores y equipos de soporte energético.'),
('Jardinería y Paisajismo', 'Equipos para el mantenimiento de áreas verdes y agricultura ligera.');

-- 2. Poblar la tabla de Máquinas (machines)
-- Se asignan máquinas a las categorías insertadas arriba usando subconsultas por nombre.
INSERT INTO machines (name, serial_number, category_id, daily_price, is_available) VALUES
-- Maquinaria Pesada
('Excavadora Oruga Cat 320', 'CAT-320-001', (SELECT id FROM categories WHERE name = 'Maquinaria Pesada'), 150000.0, true),
('Retroexcavadora John Deere 310L', 'JD-310L-042', (SELECT id FROM categories WHERE name = 'Maquinaria Pesada'), 120000.0, true),
('Rodillo Compactador Bomag', 'BMG-RC-909', (SELECT id FROM categories WHERE name = 'Maquinaria Pesada'), 85000.0, true),

-- Herramientas Eléctricas
('Taladro Percutor Makita 18V', 'MAK-TP-01', (SELECT id FROM categories WHERE name = 'Herramientas Eléctricas'), 15000.0, true),
('Demoledor Bosch GSH 11 E', 'BOS-GSH-11E', (SELECT id FROM categories WHERE name = 'Herramientas Eléctricas'), 25000.0, true),
('Sierra Circular DeWalt', 'DW-SC-700', (SELECT id FROM categories WHERE name = 'Herramientas Eléctricas'), 12000.0, false), -- No disponible inicialmente

-- Equipos de Elevación
('Plataforma Articulada JLG 450AJ', 'JLG-450-X', (SELECT id FROM categories WHERE name = 'Equipos de Elevación'), 95000.0, true),
('Montacargas Yale 3 Toneladas', 'YAL-MC-03', (SELECT id FROM categories WHERE name = 'Equipos de Elevación'), 75000.0, true),

-- Generadores y Energía
('Generador Diésel Cummins 50kVA', 'CUM-50-GEN', (SELECT id FROM categories WHERE name = 'Generadores y Energía'), 60000.0, true),
('Generador Portátil Honda EU2200i', 'HON-EU-22', (SELECT id FROM categories WHERE name = 'Generadores y Energía'), 20000.0, true),
('Compresor de Aire Sullair 185', 'SUL-185-AIR', (SELECT id FROM categories WHERE name = 'Generadores y Energía'), 45000.0, true),

-- Jardinería y Paisajismo
('Motosierra Husqvarna 450', 'HUS-450-MS', (SELECT id FROM categories WHERE name = 'Jardinería y Paisajismo'), 18000.0, true),
('Cortadora de Pasto Honda', 'HON-CP-11', (SELECT id FROM categories WHERE name = 'Jardinería y Paisajismo'), 15000.0, true);
