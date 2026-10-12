-- Fill in the seeded gallery captions so the sub-admin Gallery tab shows (and
-- edits) the same notes the public /gallery page displays, instead of "Untitled photo".
-- Captions come from gallery.notes in the static site's en locale (index order
-- matches the V39 seed); admins can change them from the dashboard from now on.
-- Public /gallery prefers row.caption and only falls back to the i18n notes.

UPDATE gallery_images AS g
SET caption = v.caption
FROM (VALUES
    (md5('vpnexues-gallery:1')::uuid,  'Women farmers at our partner fields'),
    (md5('vpnexues-gallery:2')::uuid,  'Greens growing in our fields'),
    (md5('vpnexues-gallery:3')::uuid,  'Fresh cabbage, harvested by hand'),
    (md5('vpnexues-gallery:4')::uuid,  'Seedlings shared with local farmers'),
    (md5('vpnexues-gallery:5')::uuid,  'Picking the freshest produce'),
    (md5('vpnexues-gallery:6')::uuid,  'Harvest day with our growers'),
    (md5('vpnexues-gallery:7')::uuid,  'Flowering bean crop in bloom'),
    (md5('vpnexues-gallery:8')::uuid,  'Fresh cabbage from our farms'),
    (md5('vpnexues-gallery:9')::uuid,  'Green chillies, freshly picked'),
    (md5('vpnexues-gallery:10')::uuid, 'Tender okra, hand-sorted'),
    (md5('vpnexues-gallery:11')::uuid, 'Chilli plants heavy with peppers'),
    (md5('vpnexues-gallery:12')::uuid, 'Snake gourd on the vine'),
    (md5('vpnexues-gallery:13')::uuid, 'Leafy greens in the field'),
    (md5('vpnexues-gallery:14')::uuid, 'Freshly harvested brinjal'),
    (md5('vpnexues-gallery:15')::uuid, 'Baby brinjals, farm sorted'),
    (md5('vpnexues-gallery:16')::uuid, 'Beans graded for the market'),
    (md5('vpnexues-gallery:17')::uuid, 'Ridge gourds from our fields'),
    (md5('vpnexues-gallery:18')::uuid, 'Brinjals laid out for grading'),
    (md5('vpnexues-gallery:19')::uuid, 'Green chillies sorted by hand'),
    (md5('vpnexues-gallery:20')::uuid, 'Farm-fresh cluster beans'),
    (md5('vpnexues-gallery:21')::uuid, 'Bundled greens ready to ship'),
    (md5('vpnexues-gallery:22')::uuid, 'Neat rows across our farmland'),
    (md5('vpnexues-gallery:23')::uuid, 'Healthy crops, grown naturally'),
    (md5('vpnexues-gallery:24')::uuid, 'Hand-picked leafy greens'),
    (md5('vpnexues-gallery:25')::uuid, 'Well-tended vegetable plots'),
    (md5('vpnexues-gallery:26')::uuid, 'Nutritious greens, farm to fork')
) AS v(id, caption)
WHERE g.id = v.id;
