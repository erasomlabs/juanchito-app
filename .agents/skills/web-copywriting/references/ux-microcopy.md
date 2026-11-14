# Microcopy y UX writing

Los textos pequeños deciden si la gente termina la tarea. Regla general: claridad, brevedad, y decir qué pasa después.

## Botones y enlaces

- Verbo + objeto: "Guardar cambios", "Reservar cupo", "Descargar la app", "Ver el menú".
- El botón dice lo mismo que el encabezado de la acción. Si el H2 dice "Cotiza tu evento", el botón no dice "Enviar".
- Un botón primario por vista. El resto, enlaces o botones secundarios.
- Enlaces con destino: "Ver preguntas frecuentes", no "aquí".
- Nunca "Haz clic aquí", "Más info", "Continuar" sin contexto.
- Ancho suficiente para el texto en móvil; si no cabe, el texto es muy largo.

## Formularios

- Etiqueta visible y asociada (`<label for>`). El placeholder es un ejemplo, no la etiqueta.
- Pide lo mínimo. Cada campo extra reduce finalizaciones.
- Agrupa por lógica: quién eres → qué necesitas → cómo te contactamos.
- Texto de ayuda antes del campo cuando hay formato especial.
- Campo opcional marcado como tal, no el obligatorio.
- Autocompletar y `inputmode` correctos (tel, email, numeric).
- Botón con resultado: "Enviar solicitud" y no "Enviar".

### Microcopy de campos

| Campo | Etiqueta | Ayuda | Error |
|---|---|---|---|
| Nombre | Nombre completo | Como quieres que te llamemos | Escribe tu nombre |
| Correo | Correo electrónico | Te responderemos aquí | Falta el @. Ejemplo: nombre@dominio.com |
| Teléfono | WhatsApp | Con indicativo si estás fuera de Colombia | Escribe al menos 10 dígitos |
| Fecha | Fecha del evento | Si no la tienes, elige una aproximada | Elige una fecha de hoy en adelante |
| Mensaje | Cuéntanos qué necesitas | Tipo de evento, número de personas, ciudad | Agrega una frase para orientarte |

## Errores

Fórmula: qué pasó + por qué + cómo se arregla. Sin culpar, sin códigos internos.

- ✗ Error 422: invalid input.
- ✓ El correo debe incluir @. Ejemplo: nombre@dominio.com.

- ✗ Ups, algo salió mal.
- ✓ No pudimos enviar el formulario. Revisa tu conexión e inténtalo otra vez. Si sigue, escríbenos a hola@dominio.com.

Reglas: junto al campo, no en un alert; conserva lo escrito; un error a la vez si es posible; nunca en mayúsculas ni con signos múltiples.

## Estados vacíos

Explican qué aparecerá y cuál es el siguiente paso:

- "Aún no hay eventos publicados. Los próximos se anuncian aquí y en WhatsApp."
- "Todavía no has guardado proyectos. Toca la estrella para verlos después."

No uses "No hay datos" ni un emoji triste.

## Carga

- Menos de 1 segundo: sin mensaje.
- 1 a 3 segundos: skeleton o spinner con texto si la espera es inevitable.
- Más de 3 segundos: explica qué está pasando y cuánto falta si lo sabes.
- Si falla: mensaje accionable, no "intenta más tarde" sin alternativa.

## Confirmaciones

Dicen qué ocurrió y qué sigue:

- "Solicitud enviada. Te escribimos por WhatsApp en menos de un día hábil."
- "Cambios guardados."
- "Pago recibido. Recibirás el comprobante por correo."

Sin "¡Genial!" ni confeti textual.

## WhatsApp

- Enlace con mensaje precargado: `https://wa.me/57XXXXXXXXXX?text=Hola%2C%20quiero%20cotizar%20un%20evento`.
- El mensaje debe incluir contexto, no solo "Hola": "Hola, vi la página y quiero cotizar un evento infantil para 25 niños en Pasto".
- Muestra horario de respuesta junto al botón.
- No prometas respuesta inmediata si no hay quien atienda.

## Aviso de privacidad junto al formulario

Una frase, con enlace al aviso completo:

"Usamos tus datos solo para responder tu solicitud. No los compartimos con terceros. Ver aviso de privacidad."

En Colombia, si se recolectan datos personales, aplica la Ley 1581 de 2012: finalidad, derechos del titular y canal de contacto.

## 404 y errores de página

- Reconoce: "Esta página no existe o cambió de dirección."
- Ofrece salida: enlace a inicio y a la sección más buscada.
- Sin culpar al usuario, sin bromas largas.
- Si hay búsqueda, colócala.

## Navegación y encabezados

- El nombre del enlace debe coincidir con el título de la página de destino.
- "Servicios", "Proyectos", "Contacto": sustantivos, no frases.
- El ítem activo se marca también con `aria-current="page"`.
- En móvil, el menú se llama "Menú", no un icono sin etiqueta accesible.

## Voz en pantalla

- Una sola idea por línea.
- Frases de menos de 20 palabras en móvil.
- Sin abreviaturas no explicadas.
- Sin "por favor" repetido; una vez basta.
- Sin humor en errores de pago, salud o datos personales.

## Checklist

- [ ] Cada botón dice qué hace.
- [ ] Cada campo tiene etiqueta visible.
- [ ] Cada error dice cómo se arregla.
- [ ] Cada vacío dice qué aparecerá.
- [ ] Cada confirmación dice qué sigue.
- [ ] WhatsApp tiene mensaje precargado y horario.
- [ ] Hay aviso de privacidad si se piden datos.
- [ ] La 404 ofrece salida.
- [ ] Nada esencial depende solo de un icono o color.
