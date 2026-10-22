-- Team page update:
--   * Libin K left the team, so his row is removed (his /team/libin-k URL 404s).
--   * Hemaanth Kumar T N is now the Manager. The stored detail lists are reset to
--     the Manager defaults so his profile no longer shows the Module Lead
--     content it inherited from the earlier role.
-- Both statements are idempotent: re-running them changes nothing.

DELETE FROM team_members WHERE slug = 'libin-k';

UPDATE team_members
SET role = 'Manager',
    responsibilities = '[{"title":"Team Leadership","description":"Guide team members, align priorities and support day-to-day execution."},{"title":"Delivery Management","description":"Plan the work, remove blockers and keep projects moving to schedule."},{"title":"Quality & Process","description":"Review output, uphold standards and improve how the team works."},{"title":"Stakeholder Communication","description":"Keep leadership, clients and the wider team informed with clear updates."}]',
    key_skills = '["Team Leadership","Delivery Management","Planning & Prioritisation","Quality Assurance","Mentoring","Stakeholder Communication","Problem Solving","Agile Delivery"]',
    updated_at = now()
WHERE slug = 'hemaanth-kumar-t-n';
