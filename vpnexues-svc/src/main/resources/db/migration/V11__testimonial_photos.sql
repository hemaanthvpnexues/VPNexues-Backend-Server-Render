-- Fix testimonial photos to match quotes (Small Box household vs B2B)
UPDATE testimonials SET photo_url = '/images/testimonial/testi-01.webp' WHERE name = 'Priya Nair';
UPDATE testimonials SET photo_url = '/images/testimonial/clien-01.png' WHERE name = 'James Tan';
UPDATE testimonials SET photo_url = '/images/testimonial/client-image.png' WHERE name = 'Ananya Rao';
