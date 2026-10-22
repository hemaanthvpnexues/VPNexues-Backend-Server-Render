-- Pavithra moves from Module Lead to Team Lead. Her stored detail lists are
-- empty, so the public profile now falls back to the "team lead" role content.
-- The bio is reworded so it no longer says "Module Lead".
--
-- Idempotent: absolute values, so re-running changes nothing.

UPDATE team_members
SET role = 'Team Lead',
    description = 'Pavithra is Team Lead at VPNexues, leading the team and keeping delivery on track.',
    updated_at = now()
WHERE slug = 'pavithra';
