# Administración de las webs

Copy bueno sin mantenimiento se vuelve copy falso. Esta guía define quién, cuándo y con qué evidencia se actualiza cada sitio.

## Inventario mínimo por proyecto

| Activo | Dónde vive | Dueño | Frecuencia |
|---|---|---|---|
| Posicionamiento y propuesta de valor | `.agents/copy-context.md` | Autor | Semestral |
| Textos de la home | `index.html` | Autor + agente | Trimestral |
| Precios y paquetes | `index.html` / `AGENTS.md` | Autor | Mensual o al cambiar |
| Horarios y cobertura | Home, footer, Google Business | Autor | Trimestral |
| Testimonios | Home / `02_assets/` | Autor | Al obtener uno nuevo |
| Metadatos SEO | `<head>` | Agente | Trimestral |
| README | `README.md` | Agente | Al cambiar alcance o estado |
| Redes oficiales | Footer y `sameAs` | Autor | Al crear o cerrar una cuenta |
| Aviso de privacidad | Footer | Autor | Anual o al cambiar el tratamiento |
| Enlaces externos | Home, README | Agente | Mensual (link rot) |

## Calendario

### Semanal
- Revisar mensajes de WhatsApp y correos: cada pregunta repetida es una sección que falta.
- Revisar enlaces caídos en la home y en el README (`scripts/audit-copy.js --links`).

### Mensual
- Precios, disponibilidad y horarios.
- Estado de las apps (beta, publicada, pausada).
- Una pieza de contenido nueva o actualización de una existente.
- Revisar el Search Console: consultas nuevas, páginas con impresiones y sin clics.

### Trimestral
- Reescribir el hero si la promesa cambió.
- Revisar FAQ con las preguntas reales del trimestre.
- Auditar metadatos y schema.
- Revisar accesibilidad del texto: contraste, tamaños, lectura en móvil.
- Actualizar capturas de apps.

### Semestral
- Revisar posicionamiento y competencia.
- Decidir qué se elimina. Un sitio que solo crece se vuelve ilegible.
- Revisar el tono contra el `AGENTS.md`.
- Renovar fotos de eventos o equipo.

### Anual
- Aviso de privacidad y tratamiento de datos.
- Licencias y atribuciones.
- Dominios, certificados y accesos.
- Archivar lo obsoleto en `03_exports/` o `.ignore/`.

## `.agents/copy-context.md`

Memoria de marca del proyecto. Se lee antes de escribir y se actualiza solo con decisiones aprobadas. Plantilla:

```markdown
# Copy context — {proyecto}

## Posicionamiento
- Una línea: {qué, para quién, qué lo diferencia}
- Alternativas descartadas y por qué:

## Audiencia
- Principal:
- Secundaria:
- Qué sabe antes de llegar:

## Promesa
- Resultado:
- Mecanismo:
- Pruebas disponibles:

## Objeciones
1.
2.
3.

## Voz
- Registro (tú/usted):
- Tres adjetivos con evidencia:
- Palabras propias del cliente:
- Palabras prohibidas:

## CTAs aprobados
- Primario:
- Secundario:

## Datos verificados
- Fecha de verificación:
- Fuente:

## Pendientes
- [ ] {dato faltante} — evidencia que lo cierra
```

## Reglas de gobernanza

1. **Una fuente de verdad.** Si el precio está en la home y en el README, uno de los dos debe enlazar al otro, no repetirlo.
2. **Nada se publica sin verificar.** Precio, horario, cobertura, certificación, testimonio.
3. **Cambios con fecha.** Anota en `copy-context.md` cuándo se verificó cada dato.
4. **Un responsable.** Aunque el 90 % lo genere un agente, la aprobación es humana.
5. **Lo obsoleto se archiva, no se borra.** Mover a `03_exports/` o `.ignore/` conserva el historial.
6. **Consistencia entre canales.** La bio de Instagram, la home y el README dicen lo mismo.
7. **Divulgación.** Afiliados, patrocinios y datos personales se declaran cerca de donde ocurren.

## KPIs que sí sirven

| Objetivo | Métrica | Dónde |
|---|---|---|
| Se entiende la propuesta | Rebote en la primera pantalla | Analítica |
| El CTA funciona | Clics en WhatsApp / descargas | Eventos de analítica |
| Llega gente correcta | Consultas con la intención buscada | Search Console |
| El contenido ayuda | Preguntas repetidas por mes | WhatsApp / correo |
| El README convierte | Clones o descargas / visitas | GitHub Insights |
| La app retiene | Aperturas a 7 y 30 días | Play Console |

Evita contar seguidores o "me gusta" como éxito de la web. No muestran regresión y no dependen del copy.

## Analítica mínima y privacidad

- Preferir una analítica sin cookies o con consentimiento.
- Medir eventos, no solo páginas: clic en WhatsApp, envío de formulario, descarga.
- No registrar datos personales en parámetros de URL.
- Si hay cookies no esenciales, aviso y consentimiento previo.

## Control de cambios de copy

1. Se propone el cambio en el copy deck.
2. El autor aprueba.
3. Se implementa con `senior-frontend`.
4. Se verifica en el deploy de Netlify.
5. Se anota la fecha y el motivo en `copy-context.md`.
6. Si el cambio afecta precios u oferta, se actualizan todos los canales.

## Señales de que un sitio necesita reescritura

- El H1 no dice qué se ofrece.
- El visitante pregunta por WhatsApp algo que ya está en la página.
- Los precios tienen más de seis meses.
- Hay secciones "próximamente" de hace un año.
- El texto habla de la empresa y no del cliente.
- La página creció sin que nadie haya eliminado nada.
- El README describe una versión que ya no existe.
