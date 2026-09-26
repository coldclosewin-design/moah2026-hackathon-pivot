# Round 2 illustration assets

Generated with Codex's built-in `image_gen` on 2026-09-26. Each source reference is from this repository. The generated originals remain in the local generation output folder. App exports use the required five-color palette and lossless WebP; no text or UI is embedded.

## poster_car

Reference: `docs/design/geometric-poster-development/01-setup-v1.png`

Prompt:

```text
Use case: precise-object-edit. Edit target: the supplied setup UI concept. Create ONLY the left illustration asset for an Android app, approximately 1360 x 1268 portrait-near-square aspect. Remove all text and UI, keep the illustration and color fields only. Extract/recompose the big cropped rear-three-quarter white car from the left portion, preserving its oblique perspective, navy windows, geometric panels, cropped rear at left edge, sweeping arc background. NO letters, words, numbers, brand, logo, buttons, arrows, user-interface elements or watermark. Flat crisp geometric poster art, solid colors only; use exactly this palette #070827 #FCFCFA #5B60A1 #E5E6F0 #F52D48. No gradients, shadows, texture or noise. Rightmost edge ends in solid off-white #FCFCFA so it blends into a white reading area. Keep upper left corner off-white for a brand mark that the app will draw separately. The illustration should fill the frame; do not retain the empty right half of the UI screenshot.
```

## poster_wheel_b

Reference: `docs/design/geometric-poster-development/steering-alternatives/b-oblique-rim-v1.png`

Prompt:

```text
Use case: precise-object-edit. Edit target: the supplied steering-wheel B UI concept. Create ONLY the right illustration asset for an Android app, approximately 1100 x 1268 portrait aspect. Remove all text and UI, keep the illustration and color fields only. Reframe just the large oblique cross-section steering wheel from the right portion on a solid #070827 navy background. Preserve the B wheel's elliptical perspective, broad rim sidewall, center pad, two spokes and simple geometric controls, bold asymmetrical crop beyond the right and bottom edges. Do not draw the left white reading area, labels, glyphs, numbers, letters, logo, buttons as UI, or watermark. Flat crisp geometric poster illustration in exactly five solid colors #070827 #FCFCFA #5B60A1 #E5E6F0 #F52D48 only. No gradients, texture, noise, shadow, bevel or photorealistic material. Wheel decorative inset control shapes can remain, without symbols. Fill the full frame with navy and the cropped wheel.
```

Car refinement prompt:

```text
Use case: precise-object-edit. Edit this car illustration asset only. Keep the car shape, perspective and geometric composition intact. Make the entire RIGHTMOST 7 percent vertical strip solid off-white #FCFCFA from top to bottom: terminate the purple road bar, navy road arc and every shape before that strip; make their ends smooth curves meeting the off-white. This is essential to seamlessly join a white app reading pane. Increase the solid off-white empty upper-left corner height to 200 pixels (about 16 percent of the square image height), width at least 420 pixels: it must have space for a brand mark drawn separately, but DO NOT draw any text. Use exactly 5 FLAT solid colors #070827 #FCFCFA #5B60A1 #E5E6F0 #F52D48; remove subtle shading, texture, gradients and noise from all flat panels. No text, letters, logos, numerals, UI, buttons, watermark. Keep the large cropped white rear-three-quarter car, red taillight, navy background masses and periwinkle arc. Output just this illustration, near-square landscape approx1360x1268.
```

Exports: `automotive/src/main/res/drawable-nodpi/poster_car.webp` (1360 × 1268), `poster_wheel_b.webp` (1100 × 1268). Nearest-palette export removes generator color drift, then saves losslessly. Both images are decorative (`contentDescription = null`).

Verified export sizes: car 11,574 bytes, wheel 10,856 bytes. Decoded WebP files each contain exactly five colors, all from the required palette.
