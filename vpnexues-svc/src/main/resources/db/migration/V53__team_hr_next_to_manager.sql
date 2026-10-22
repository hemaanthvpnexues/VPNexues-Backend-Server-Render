-- Team grid order: the HR members now sit directly to the right of the Manager
-- (Hemaanth Kumar T N) instead of further down the list.
--
-- Before: hemaanth 3, sathish 4, monika 5, sunil 6, jegatheeswaran 7,
--         keerthi 8, shereen 9, kiruba 10
-- After:  hemaanth 3, shereen 4, kiruba 5, sathish 6, monika 7, sunil 8,
--         jegatheeswaran 9, keerthi 10
--
-- Absolute display_order values are assigned (not relative shifts), so the
-- migration is idempotent. Members added later through the admin dashboard
-- keep their own display_order and stay below the seeded ones.

UPDATE team_members
SET display_order = CASE slug
    WHEN 'shereen'          THEN 4
    WHEN 'kiruba'           THEN 5
    WHEN 'sathish-r'        THEN 6
    WHEN 'monika-v'         THEN 7
    WHEN 'sunil-kumar-r'    THEN 8
    WHEN 'jegatheeswaran'   THEN 9
    WHEN 'keerthi-vardhan'  THEN 10
END
WHERE slug IN ('shereen', 'kiruba', 'sathish-r', 'monika-v', 'sunil-kumar-r',
               'jegatheeswaran', 'keerthi-vardhan');
