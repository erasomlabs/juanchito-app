# SEO Copy — lo que depende del texto

El SEO técnico es de `senior-frontend`. Aquí se define qué palabras y qué estructura textual sostienen la intención de búsqueda.

## Campos por página

| Campo | Longitud | Regla |
|---|---|---|
| `<title>` | 50-60 caracteres | Intención principal + marca. Sin relleno. |
| `meta description` | 140-155 | Beneficio + prueba + acción implícita. Se escribe para el clic. |
| `og:title` | 40-60 | Puede diferir del title; más humano. |
| `og:description` | 100-150 | Una frase con el resultado. |
| `H1` | 1 por página | Coincide con la promesa del title, sin repetirlo literal. |
| `H2` | 3-6 | Cada uno cubre una intención o pregunta. |
| `slug` | 3-5 palabras | Minúsculas, guiones, sin preposiciones. |

## Intención de búsqueda

Antes de escribir, clasifica la página:

- **Informacional**: "cómo acompañar el duelo en niños". Contenido que responde, con enlaces a servicios.
- **Comparación**: "traducción certificada vs. traducción simple". Tabla honesta.
- **Transaccional local**: "organizador de eventos infantiles Pasto". Hero con ciudad, cobertura y CTA.
- **Navegacional**: "Juanchito Recreaciones". La home debe confirmar la marca en el H1 o subtítulo.
- **Producto**: "localizador Bluetooth precio". Ficha con beneficio, precio y enlace.

Una página no debe pelear por dos intenciones distintas.

## Estructura textual

1. Keyword principal en el `title`, el `H1` y el primer párrafo. Una vez cada uno, natural.
2. Variantes semánticas en `H2` y cuerpo: sinónimos, ciudad, tipo de servicio, público.
3. Párrafos de 2 a 4 líneas; listas cuando hay pasos o comparación.
4. Enlaces internos con ancla descriptiva: "ver precios de eventos infantiles", no "ver más".
5. Cierre con CTA y datos de contacto visibles en texto (no solo en imagen).
6. `alt` descriptivo en imágenes de contenido; `alt=""` en decorativas.

## Keywords por proyecto

Punto de partida; valida con datos reales antes de fijarlas.

### juanchito
- Principal: organizador de eventos infantiles en Pasto.
- Secundarias: fiestas infantiles Pasto, animación para eventos, eventos corporativos Nariño, BTL Pasto, pastelería para eventos.
- Long tail: "cuánto cuesta una fiesta infantil en Pasto", "animación con teatro para niños".

### semillasdevida
- Principal: psicología para niños y familias en Pasto.
- Secundarias: educación emocional, taller para padres, apoyo psicológico escolar, manejo de emociones en niños.
- Long tail: "cómo ayudar a mi hijo con ansiedad", "talleres de educación emocional para colegios".
- Cuidado: no competir por términos clínicos que impliquen tratamiento si no se ofrece.

### volcanotranslators
- Principal: traducción certificada para trámites migratorios.
- Secundarias: traducción de documentos Estados Unidos, traducción jurada español-inglés, traducción para visa.
- Long tail: "traducir acta de nacimiento para green card", "traducción de documentos para USCIS".
- Cuidado: no afirmar aceptación garantizada por una entidad específica.

### sebaserasom
- Principal: Juan Sebastián Eraso Meléndez / Sebas Eraso M.
- Secundarias: psicólogo humanista Pasto, organizador de proyectos, portafolio profesional Nariño.
- Navegacional: el nombre debe aparecer en title, H1 y schema.

### erasomlabs
- Principal: Eraso M Labs, desarrollo de apps Android.
- Secundarias: apps Android Colombia, laboratorio de software, desarrollo Kotlin.
- Long tail: nombre de cada app + "Android".

### tienda.coolstuff (EN)
- Principal: cool gadgets and design finds.
- Secundarias: unique tech accessories, creative gifts, useful desk gadgets.
- Long tail: "best bluetooth tracker for keys", "gift ideas for tech lovers".
- Evita términos de marca registrada en el title.

### zonadetv
- Principal: app para ver TV deportiva en Android.
- Secundarias: ver deportes en el celular, reproductor IPTV Android.
- Cuidado: no posicionar sobre contenido no autorizado ni prometer canales específicos.

## Datos estructurados (JSON-LD)

Solo si el negocio existe y los datos son reales.

- **LocalBusiness** (juanchito, semillasdevida): nombre, dirección, teléfono, horario, área servida, mismo `@id` en todas las páginas.
- **Organization** (sebaserasom, erasomlabs): nombre, url, logo, sameAs (redes oficiales).
- **Product** (coolstuff): nombre, imagen, descripción, oferta con precio y moneda, disponibilidad. Si el precio está en el proveedor, no inventes uno.
- **FAQPage**: solo preguntas visibles en la página y respuestas idénticas.
- **SoftwareApplication** (apps): nombre, sistema operativo, categoría, precio (0 si es gratis).

No dupliques schema en la misma página. Valida en el Rich Results Test.

## Consistencia local (NAP)

Nombre, dirección y teléfono deben ser idénticos en la web, Google Business Profile, redes y directorios. Una diferencia de formato rompe la señal.

- Nombre: exactamente como figura en el registro.
- Dirección: formato único, con ciudad y departamento.
- Teléfono: un solo número principal, con indicativo.

## Contenido que sostiene el SEO

- Una página por servicio o intención, no una sola página con todo.
- 400 a 800 palabras útiles en páginas de servicio; más solo si hay algo que decir.
- Preguntas frecuentes reales, tomadas de WhatsApp y correos.
- Casos o ejemplos con permiso del cliente.
- Actualización al menos semestral de precios, horarios y cobertura.

## Errores

- Repetir la keyword hasta hacer ilegible el texto.
- Meta description que empieza con "Bienvenido a nuestra página web".
- Title distinto en cada página pero todos genéricos.
- Varias páginas compitiendo por la misma keyword sin canonical.
- Texto oculto, listas de ciudades sin contenido real.
- Enlaces internos todos a la home.
- `alt` relleno de keywords.

## Checklist

- [ ] Title y meta con longitud correcta y distintos por página.
- [ ] Un H1 por página, alineado con la intención.
- [ ] Keyword en title, H1 y primer párrafo, sin repetición forzada.
- [ ] H2 que cubren preguntas reales.
- [ ] Enlaces internos descriptivos.
- [ ] `alt` útil en imágenes de contenido.
- [ ] JSON-LD solo con datos verificados.
- [ ] NAP consistente.
- [ ] `og:image` de al menos 1200×630.
- [ ] Sin páginas duplicadas ni contenido de relleno.
