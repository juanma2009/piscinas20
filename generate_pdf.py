#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from fpdf import FPDF
import datetime

# fpdf2 uses FPDF from fpdf module

class PDF(FPDF):
    def header(self):
        self.set_font('Helvetica', 'B', 12)
        self.set_text_color(0, 51, 102)
        self.cell(0, 10, 'Análisis de Aplicación - piscinas20', 0, 1, 'C')
        self.ln(3)
    
    def footer(self):
        self.set_y(-15)
        self.set_font('Helvetica', 'I', 8)
        self.set_text_color(128, 128, 128)
        self.cell(0, 10, f'Página {self.page_no()}/{{nb}}', 0, 0, 'C')
    
    def chapter_title(self, title):
        self.set_font('Helvetica', 'B', 14)
        self.set_text_color(0, 51, 102)
        self.cell(0, 10, title, 0, 1, 'L')
        self.set_draw_color(0, 102, 153)
        self.line(10, self.get_y(), 200, self.get_y())
        self.ln(5)
    
    def section_title(self, title):
        self.set_font('Helvetica', 'B', 11)
        self.set_text_color(0, 102, 153)
        self.cell(0, 8, title, 0, 1, 'L')
        self.ln(2)
    
    def body_text(self, text):
        self.set_font('Helvetica', '', 10)
        self.set_text_color(0, 0, 0)
        self.multi_cell(0, 6, text)
        self.ln(3)
    
    def bullet_point(self, text, indent=15):
        self.set_font('Helvetica', '', 10)
        self.set_text_color(0, 0, 0)
        x = self.get_x()
        self.cell(indent, 6, '-', 0, 0)
        self.multi_cell(0, 6, text)
        self.ln(1)

pdf = PDF()
pdf.alias_nb_pages()
pdf.set_auto_page_break(auto=True, margin=20)

# Cover page
pdf.add_page()
pdf.ln(40)
pdf.set_font('Helvetica', 'B', 28)
pdf.set_text_color(0, 51, 102)
pdf.cell(0, 20, 'Análisis de Aplicación', 0, 1, 'C')
pdf.ln(5)
pdf.set_font('Helvetica', '', 20)
pdf.set_text_color(0, 102, 153)
pdf.cell(0, 15, 'piscinas20', 0, 1, 'C')
pdf.ln(10)
pdf.set_font('Helvetica', '', 14)
pdf.set_text_color(80, 80, 80)
pdf.cell(0, 10, 'Sistema de Gestión de Taller de Joyería', 0, 1, 'C')
pdf.ln(20)
pdf.set_font('Helvetica', 'I', 10)
pdf.cell(0, 8, f'Fecha: {datetime.datetime.now().strftime("%d/%m/%Y")}', 0, 1, 'C')

# Summary
pdf.add_page()
pdf.chapter_title('RESUMEN GENERAL')
pdf.body_text(
    'Esta es una aplicación de gestión de taller de joyería construida con Spring Boot. '
    'Aunque el nombre del proyecto es "piscinas20", en realidad es un sistema completo '
    'para administrar un negocio de reparación y fabricación de joyas. La aplicación gestiona '
    'pedidos, clientes, citas, albaranes, facturas, proveedores, materiales y seguimiento '
    'financiero del negocio.'
)

# Tech Stack
pdf.chapter_title('STACK TECNOLÓGICO')

pdf.section_title('Backend')
pdf.bullet_point('Framework: Spring Boot 2.5.6')
pdf.bullet_point('Lenguaje: Java 17')
pdf.bullet_point('ORM: Spring Data JPA')
pdf.bullet_point('Seguridad: Spring Security con autenticación basada en roles')
pdf.bullet_point('Email: Spring Mail')
pdf.ln(3)

pdf.section_title('Frontend')
pdf.bullet_point('Plantillas: Thymeleaf (con dialecto de layout)')
pdf.bullet_point('UI Framework: AdminLTE (basado en Bootstrap)')
pdf.bullet_point('JavaScript: jQuery + jQuery UI')
pdf.bullet_point('Drag & Drop: Sortable.js')
pdf.ln(3)

pdf.section_title('Base de Datos')
pdf.bullet_point('Producción: PostgreSQL (AWS RDS)')
pdf.bullet_point('Testing: H2 (en memoria)')
pdf.ln(3)

pdf.section_title('Cache')
pdf.bullet_point('Remoto: Redis (Redis Labs) - cola asíncrona para subida de archivos')
pdf.bullet_point('Local: Caffeine (cache en memoria)')
pdf.ln(3)

pdf.section_title('Reportes')
pdf.bullet_point('JasperReports 7.0.4 - generación de reportes PDF')
pdf.ln(3)

pdf.section_title('Build & Herramientas')
pdf.bullet_point('Maven (gestor de dependencias)')
pdf.bullet_point('Lombok (reducción de código boilerplate)')
pdf.bullet_point('MapStruct (mapeo DTO/entidad)')
pdf.bullet_point('Thumbnailator (compresión de imágenes)')
pdf.ln(3)

pdf.section_title('Testing')
pdf.bullet_point('JUnit 5 / Spring Security Test')
pdf.bullet_point('Playwright (testing E2E de navegador)')

# External Integrations
pdf.add_page()
pdf.chapter_title('INTEGRACIONES EXTERNAS')

pdf.set_font('Helvetica', '', 10)
pdf.set_text_color(0, 0, 0)

integrations = [
    ('PostgreSQL (AWS RDS)', 'Base de datos principal', 'SPRING_DATASOURCE_*'),
    ('Redis (Redis Labs)', 'Cache + cola asíncrona', 'spring.redis.url'),
    ('Cloudinary', 'Almacenamiento de archivos/imágenes', 'CLOUDINARY_*'),
    ('Google Drive API', 'Selector de archivos y autenticación', 'GOOGLE_OAUTH_CLIENT_ID'),
    ('Google Calendar', 'Sincronización de citas', 'google.calendar.enabled'),
    ('Infobip', 'SMS y autenticación 2FA', 'INFOBIP_*'),
    ('Gmail SMTP', 'Notificaciones por email', 'MAIL_USER / MAIL_PASS'),
]

pdf.set_font('Helvetica', 'B', 10)
pdf.cell(45, 8, 'Servicio', 1, 0, 'C')
pdf.cell(65, 8, 'Propósito', 1, 0, 'C')
pdf.cell(70, 8, 'Configuración', 1, 1, 'C')

pdf.set_font('Helvetica', '', 9)
for service, purpose, config in integrations:
    pdf.cell(45, 8, service, 1, 0)
    pdf.cell(65, 8, purpose, 1, 0)
    pdf.cell(70, 8, config, 1, 1)
pdf.ln(5)

# Main Features
pdf.chapter_title('FUNCIONALIDADES PRINCIPALES')

features = [
    'Gestión de Pedidos (PedidoController) - Órdenes del taller con seguimiento completo y fotos adjuntas',
    'Clientes - CRM completo para clientes del taller',
    'Citas - Calendario de citas con integración a Google Calendar',
    'Albaranes y Facturas - Generación de documentación de entrega y facturación',
    'Balance Financiero - Seguimiento de ingresos/gastos (GastoApp/IngresoEmpresa)',
    'Checklist de Taller - Control de calidad para procesos del taller',
    'Subida de Archivos - Fotos adjuntas a pedidos (Cloudinary + Google Drive)',
    'Autenticación 2FA - Verificación por SMS mediante Infobip',
    'Proveedores y Materiales - Gestión de inventario',
    'Notificaciones - Sistema de alertas para usuarios',
    'Métodos de Pago - Gestión de formas de pago',
    'Comentarios - Notas y comentarios en pedidos',
    'Configuración - Gestión de metales, servicios, estados y grupos',
    'Administración Avanzada - Operaciones exclusivas de SuperAdmin',
]

for feature in features:
    pdf.bullet_point(feature)

# Architecture
pdf.add_page()
pdf.chapter_title('ARQUITECTURA')

pdf.section_title('Arquitectura en Capas (MVC)')
pdf.body_text(
    'Controllers (30+ controladores)\n'
    '    |\n'
    '    v\n'
    'Services (lógica de negocio)\n'
    '    |\n'
    '    v\n'
    'DAOs/Repositories (Spring Data JPA)\n'
    '    |\n'
    '    v\n'
    'Entities (modelos de dominio JPA)'
)

pdf.section_title('Entidades Principales')
pdf.body_text(
    'Pedido - Órdenes de trabajo (entidad central)\n'
    'Cliente - Clientes del taller\n'
    'Cita / CitaHistorial - Citas y su historial\n'
    'Albaran / ItemAlbaran - Documentos de entrega con líneas\n'
    'Factura / ItemFactura - Facturas con líneas\n'
    'ArchivoAdjunto - Fotos adjuntas almacenadas en Cloudinary\n'
    'Comentario - Comentarios/notas en pedidos\n'
    'Proveedor - Proveedores\n'
    'Material / Producto - Inventario de materiales\n'
    'ChecklistTemplate / ChecklistInstance - Plantillas de control de calidad\n'
    'OrderStone - Seguimiento de piedras en pedidos\n'
    'User / Role - Autenticación con Spring Security\n'
    'Notificacion - Notificaciones de usuario\n'
    'GastoApp / IngresoEmpresa / Empresa - Seguimiento financiero'
)

pdf.section_title('Controladores Principales')
pdf.body_text(
    'PedidoController - Gestión principal de órdenes/trabajos del taller\n'
    'ClienteController - CRUD de clientes\n'
    'CitaController - Programación de citas\n'
    'AlbaranController - Notas de entrega\n'
    'FacturaController - Facturación\n'
    'ProveedorController - Gestión de proveedores\n'
    'MaterialController - Materiales/inventario\n'
    'WorkshopController - Gestión de checklists del taller\n'
    'BalanceController - Balance financiero y reportes\n'
    'NotificationController - Notificaciones de usuario\n'
    'GoogleOAuthController - Flujo OAuth\n'
    'GoogleDriveController - Gestión de archivos Drive\n'
    'TwoFactorController - Autenticación 2FA\n'
    'SuperAdminController - Operaciones exclusivas de admin\n'
    'ConfiguracionController - Configuración (metales, servicios, estados, grupos)'
)

# Deployment
pdf.add_page()
pdf.chapter_title('DESPLEGUE')

pdf.section_title('Dockerfile')
pdf.body_text(
    'Build multi-etapa: Maven builder + Eclipse Temurin 11 runtime\n'
    'Puerto expuesto: 8080'
)

pdf.section_title('Render.com (render.yaml)')
pdf.body_text(
    'Desplegado como servicio Docker con:\n'
    '  - Nombre del servicio: joyeria-app\n'
    '  - Plan: Free tier\n'
    '  - Base de datos PostgreSQL vinculada: joyeria-db\n'
    '  - Entorno: SPRING_PROFILES_ACTIVE=prod'
)

pdf.section_title('Heroku')
pdf.body_text(
    'Compatible mediante Procfile:\n'
    'web: java -Dserver.port=$PORT -jar ...'
)

pdf.section_title('Perfiles de Spring')
pdf.bullet_point('application.properties - Configuración por defecto')
pdf.bullet_point('application-dev.properties - Desarrollo')
pdf.bullet_point('application-prod.properties - Producción')
pdf.bullet_point('application-test.properties - Testing')

# Security Concerns
pdf.ln(5)
pdf.chapter_title('PROBLEMA DE SEGURIDAD DETECTADO')

pdf.set_fill_color(255, 230, 230)
pdf.set_text_color(180, 0, 0)
pdf.set_font('Helvetica', 'B', 10)
pdf.multi_cell(0, 8, 
    'ADVERTENCIA: El archivo configVaraiblesEntorno.env.example contiene '
    'credenciales reales (contraseñas de BD, secretos de Cloudinary, Google OAuth, '
    'etc.). Estas credenciales deben ser ROTADAS INMEDIATAMENTE y nunca deben ser '
    'comprometidas en el repositorio.',
    border=1, fill=True)

# Mobile App
pdf.ln(5)
pdf.chapter_title('APLICACIÓN MÓVIL')
pdf.body_text(
    'Existe una aplicación Android compañera en el directorio android-app/ con su '
    'propio build Gradle, que probablemente sirve como complemento móvil para el '
    'sistema de gestión del taller.'
)

# Final Summary
pdf.add_page()
pdf.chapter_title('RESUMEN FINAL')
pdf.body_text(
    'Se trata de un sistema de gestión de taller de joyería production-grade con '
    'funcionalidades completas de órdenes, clientes, facturación, archivos adjuntos, '
    'autenticación segura y reporting financiero. La aplicación es desplegable en la '
    'nube vía Docker (Render.com o Heroku) y cuenta con una arquitectura bien estructurada '
    'con más de 30 controladores, integración con servicios externos (Google Drive, '
    'Google Calendar, Cloudinary, Infobip SMS), procesamiento asíncrono con Redis, y '
    'una interfaz web rica basada en AdminLTE.\n\n'
    'Características destacables:\n'
    '  - Patrón de subida en dos pasos para evitar timeouts\n'
    '  - Cola Redis para procesamiento asíncrono de archivos\n'
    '  - Autenticación OAuth 2.0 con Google Drive\n'
    '  - Seguridad con Spring Security y soporte 2FA\n'
    '  - Multi-entorno: desarrollo local, Heroku y Render\n'
    '  - Optimización de imágenes con Thumbnailator'
)

# Save PDF
output_path = r'C:\Users\Juanma\IdeaProjects\piscinas20\analisis_aplicacion.pdf'
pdf.output(output_path)
print(f'PDF generado exitosamente en: {output_path}')
