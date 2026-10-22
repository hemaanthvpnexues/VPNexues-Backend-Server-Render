-- Career job detail fields: the public /careers page renders an icon,
-- responsibilities, requirements, perks and (for some roles) several openings,
-- so the sub-admin CRUD needs them too.
--
-- Also seeds the 15 jobs that were hardcoded in
-- vpnexues-ui/src/pages/marketing/Careers.tsx, so the public page can switch to
-- API-backed content and render identically afterwards.
--
-- Storage format (mirrors team_members, V37):
--   skills           -> comma-separated text (same format the admin form edits)
--   responsibilities -> JSON array of strings
--   requirements     -> JSON array of strings
--   perks            -> JSON array of strings
--   openings         -> JSON array of {role, type, responsibilities[]}
--
-- ids are deterministic md5 UUIDs, so the same rows land on every machine.

ALTER TABLE career_jobs ADD COLUMN IF NOT EXISTS slug VARCHAR(100);
ALTER TABLE career_jobs ADD COLUMN IF NOT EXISTS icon VARCHAR(100);
ALTER TABLE career_jobs ADD COLUMN IF NOT EXISTS responsibilities TEXT;
ALTER TABLE career_jobs ADD COLUMN IF NOT EXISTS requirements TEXT;
ALTER TABLE career_jobs ADD COLUMN IF NOT EXISTS perks TEXT;
ALTER TABLE career_jobs ADD COLUMN IF NOT EXISTS openings TEXT;

-- Backfill a slug for any row created before this migration, then lock it down.
-- The slug is the public link key (/careers/apply?job=<slug>) and the lookup key
-- for the 5-language job translations, so it must never change once set.
UPDATE career_jobs
SET slug = trim(both '-' FROM regexp_replace(lower(title), '[^a-z0-9]+', '-', 'g'))
WHERE slug IS NULL;

ALTER TABLE career_jobs ALTER COLUMN slug SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_career_jobs_slug ON career_jobs (slug);

-- ---------------------------------------------------------------------------
-- Seed: the static site's open positions (JOBS in Careers.tsx)
-- ---------------------------------------------------------------------------
INSERT INTO career_jobs (
    id, slug, title, type, location, icon, description, skills,
    duration, stipend, start_date, about,
    responsibilities, requirements, perks, openings,
    active, display_order)
VALUES
(md5('vpnexues-career-job:fullstack')::uuid, 'fullstack', 'Full Stack Web Developer Intern', 'Internship', 'Remote', 'fa-solid fa-laptop-code',
 'Build next-generation dashboards and farmer portals end to end — Node.js + React on the frontend, Java on the backend, and MongoDB for data. Freshers welcome.',
 'Node.js, React, Java, MongoDB', '6 Months', 'Based on the performance', 'ASAP',
 'Join our engineering team to build real products that connect farmers to global markets. You''ll work on dashboards, data pipelines, and customer-facing features.',
 '["Build and maintain web applications","Write clean, tested code","Collaborate with designers and PMs","Participate in code reviews"]',
 '["Familiarity with JavaScript/TypeScript","Basic understanding of React or Node.js","Eagerness to learn","Good communication skills"]',
 '["🌱 Farm Fresh Produce","📚 Learning Budget","🏠 Remote-First","🌴 Team Retreats","💰 Growth Bonuses","👨‍🏫 Mentorship"]',
 NULL, TRUE, 0),

(md5('vpnexues-career-job:seo')::uuid, 'seo', 'SEO Content Writer', 'Fresher', 'Remote', 'fa-solid fa-pen-nib',
 'Create SEO-optimized blog posts, product descriptions, and website content that drive organic traffic and showcase our organic produce to global audiences.',
 'SEO Writing, Keyword Research, Content Strategy, Lead Generation', '6 Months', 'Based on the performance', 'ASAP',
 'Lead our content marketing efforts with compelling, SEO-driven content that tells the story of our organic produce and connects with customers worldwide.',
 NULL,
 '["Strong writing skills","SEO best practices","Research ability","Attention to detail"]',
 '["🌱 Farm Fresh Produce","📚 Learning Budget","🏠 Remote-First","🌴 Team Retreats","💰 Growth Bonuses","👨‍🏫 Mentorship"]',
 '[{"role":"SEO Content Writer","type":"Fresher","responsibilities":["Write SEO-optimized blog posts","Create product descriptions","Develop website content","Conduct keyword research","Analyze content performance"]},{"role":"SEO Lead Generation","type":"Full-Time","responsibilities":["Conduct prospect research","Perform keyword research for leads","Generate organic leads","Maintain lead database","Track SEO-driven conversions"]}]',
 TRUE, 1),

(md5('vpnexues-career-job:sales')::uuid, 'sales', 'Sales & Marketing Executive', 'Fresher', 'On-Site', 'fa-solid fa-bullhorn',
 'Drive B2B and retail sales while executing marketing campaigns to promote our organic products across India and international markets.',
 'B2B Sales, Digital Marketing, Client Relations', '6 Months', '₹10,000 - ₹15,000', 'ASAP',
 'Drive revenue growth through B2B partnerships and retail channels while building brand awareness through strategic marketing campaigns.',
 NULL,
 '["Excellent communication skills","Comfortable with targets","Basic computer skills","Willingness to travel locally"]',
 '["🌱 Farm Fresh Produce","📚 Learning Budget","🌴 Team Retreats","💰 Growth Bonuses","👨‍🏫 Mentorship","🌍 Global Impact"]',
 '[{"role":"Sales & Marketing Executive","type":"Fresher","responsibilities":["Identify and close B2B deals","Execute marketing campaigns","Build client relationships","Track sales metrics","Drive business development"]},{"role":"Sales & Marketing Intern","type":"Internship","responsibilities":["Support sales outreach","Assist with campaigns","Engage customers","Generate leads","Support marketing activities"]},{"role":"B2B / Retail Sales Intern","type":"Internship","responsibilities":["Assist with B2B outreach","Handle retail customer inquiries","Follow up on leads","Maintain customer database","Support product inquiries"]}]',
 TRUE, 2),

(md5('vpnexues-career-job:agriculture')::uuid, 'agriculture', 'Agriculture Executive', 'Full-Time', 'On-Site', 'fa-solid fa-tractor',
 'Manage farmer partnerships, coordinate organic certifications, and ensure quality supply chain operations for our export business.',
 'Farm Management, Supply Chain, Organic Certification, Crop Monitoring', '6 Months', 'Based on the performance', 'ASAP',
 'Work directly with farming communities to ensure quality organic produce meets international standards for our export operations.',
 NULL,
 '["Degree in Agriculture or related field","Understanding of organic certification","Good coordination skills","Willingness to work on farms"]',
 '["🌱 Farm Fresh Produce","📚 Learning Budget","🌴 Team Retreats","💰 Growth Bonuses","👨‍🏫 Mentorship","🌍 Global Impact"]',
 '[{"role":"Agriculture Executive","type":"Full-Time","responsibilities":["Manage farmer relationships","Coordinate organic certifications","Ensure quality standards","Optimize supply chain","Lead farm operations"]},{"role":"Agriculture Intern","type":"Internship","responsibilities":["Monitor crop health","Coordinate with farmers","Support organic practices","Document farm activities","Assist with produce handling"]}]',
 TRUE, 3),

(md5('vpnexues-career-job:hr')::uuid, 'hr', 'HR Executive', 'Full-Time', 'Remote & On-Site', 'fa-solid fa-users',
 'Manage recruitment, employee relations, and HR operations to build and sustain a motivated team across our farm and office locations.',
 'Recruitment, Employee Relations, HR Operations, Documentation', '6 Months', 'Based on the performance', 'ASAP',
 'Build and sustain a world-class team by managing recruitment, employee engagement, and HR operations across our locations.',
 NULL,
 '["Recruitment experience","Good interpersonal skills","Knowledge of HR policies","Strong documentation skills"]',
 '["🌱 Farm Fresh Produce","📚 Learning Budget","🌴 Team Retreats","💰 Growth Bonuses","👨‍🏫 Mentorship","🌍 Global Impact"]',
 '[{"role":"HR Executive","type":"Full-Time","responsibilities":["Manage end-to-end recruitment","Handle employee relations","Implement HR policies","Organize team events","Lead HR operations"]},{"role":"HR Intern","type":"Internship","responsibilities":["Screen resumes","Coordinate interviews","Maintain employee records","Assist with HR admin","Support onboarding documentation"]}]',
 TRUE, 4),

(md5('vpnexues-career-job:accountant')::uuid, 'accountant', 'Accountant', 'Fresher', 'On-Site', 'fa-solid fa-calculator',
 'Manage bookkeeping, invoicing, and financial records to keep our farm-to-table export operations running smoothly and profitably.',
 'Tally, GST, Bookkeeping, MS Excel', '6 Months', 'Based on the performance', 'ASAP',
 'Keep our financial operations running smoothly by managing books, invoices, and compliance for our growing export business.',
 '["Maintain financial records","Process invoices","Handle GST compliance","Generate financial reports"]',
 '["Commerce background","Tally/Excel proficiency","Attention to detail","Basic accounting knowledge"]',
 '["🌱 Farm Fresh Produce","📚 Learning Budget","🌴 Team Retreats","💰 Growth Bonuses","👨‍🏫 Mentorship","🌍 Global Impact"]',
 NULL, TRUE, 5),

(md5('vpnexues-career-job:customer-support')::uuid, 'customer-support', 'Customer Support Intern', 'Internship', 'Remote', 'fa-solid fa-headset',
 'Support customers with product inquiries, orders, delivery questions and service requests while maintaining a professional customer experience.',
 'Customer Support, Communication, Order Support, Problem Solving', '6 Months', 'Based on the performance', 'ASAP',
 'Be the first point of contact for our customers, ensuring they receive prompt and professional support for all their queries.',
 '["Handle customer inquiries","Process order requests","Resolve service issues","Document customer feedback"]',
 '["Excellent communication","Patient and empathetic","Basic computer skills","Problem-solving attitude"]',
 '["🌱 Farm Fresh Produce","📚 Learning Budget","🏠 Remote-First","🌴 Team Retreats","👨‍🏫 Mentorship"]',
 NULL, TRUE, 6),

(md5('vpnexues-career-job:supply-chain')::uuid, 'supply-chain', 'Supply Chain Intern', 'Internship', 'Hybrid & On-Site', 'fa-solid fa-truck-fast',
 'Assist with inventory coordination, supplier communication, order movement and day-to-day farm-to-customer supply chain operations.',
 'Supply Chain, Inventory, Logistics, Coordination', '6 Months', 'Based on the performance', 'ASAP',
 'Learn the end-to-end supply chain process from farm to customer, ensuring timely delivery of fresh organic produce.',
 '["Track inventory levels","Coordinate with suppliers","Monitor order shipments","Update supply chain data"]',
 '["Organizational skills","Basic Excel knowledge","Attention to detail","Willing to work on-site"]',
 '["🌱 Farm Fresh Produce","📚 Learning Budget","🌴 Team Retreats","💰 Growth Bonuses","👨‍🏫 Mentorship"]',
 NULL, TRUE, 7),

(md5('vpnexues-career-job:ea-director')::uuid, 'ea-director', 'Executive Assistant to Director', 'Full-Time', 'Remote & On-Site', 'fa-solid fa-briefcase',
 'Support the Director with scheduling, meeting coordination, documentation, communication and follow-up of important business activities.',
 'Scheduling, Documentation, Communication, Coordination', '6 Months', 'Based on the performance', 'ASAP',
 'Be the right hand to our Director, managing their schedule and ensuring smooth day-to-day operations.',
 '["Manage director calendar","Coordinate meetings","Prepare documents","Handle correspondence"]',
 '["Excellent organizational skills","Strong communication","MS Office proficiency","Discretion and professionalism"]',
 '["🌱 Farm Fresh Produce","📚 Learning Budget","🌴 Team Retreats","💰 Growth Bonuses","👨‍🏫 Mentorship"]',
 NULL, TRUE, 8),

(md5('vpnexues-career-job:pa-director')::uuid, 'pa-director', 'Personal Assistant to Director', 'Full-Time', 'Remote & On-Site', 'fa-solid fa-user-tie',
 'Assist the Director with daily schedules, correspondence, meeting preparation, reminders and administrative coordination.',
 'Time Management, Communication, Scheduling, Administration', '6 Months', 'Based on the performance', 'ASAP',
 'Provide comprehensive personal and administrative support to ensure the Director operates efficiently.',
 '["Manage daily schedules","Handle personal correspondence","Set reminders","Coordinate travel"]',
 '["Strong time management","Excellent communication","Proactive attitude","High level of organization"]',
 '["🌱 Farm Fresh Produce","📚 Learning Budget","🌴 Team Retreats","💰 Growth Bonuses","👨‍🏫 Mentorship"]',
 NULL, TRUE, 9),

(md5('vpnexues-career-job:quality')::uuid, 'quality', 'Produce Quality / Food Quality Intern', 'Internship', 'On-Site', 'fa-solid fa-magnifying-glass-chart',
 'Assist in inspecting fresh produce, maintaining quality standards, checking handling practices and supporting food-quality documentation.',
 'Quality Inspection, Food Quality, Documentation, Fresh Produce', '6 Months', 'Based on the performance', 'ASAP',
 'Ensure our organic produce meets the highest quality standards through inspection and documentation.',
 '["Inspect fresh produce","Document quality checks","Monitor handling practices","Support quality reports"]',
 '["Attention to detail","Basic food safety knowledge","Good documentation skills","Willing to work on-site"]',
 '["🌱 Farm Fresh Produce","📚 Learning Budget","🌴 Team Retreats","💰 Growth Bonuses","👨‍🏫 Mentorship"]',
 NULL, TRUE, 10),

(md5('vpnexues-career-job:procurement')::uuid, 'procurement', 'Procurement Intern', 'Internship', 'On-Site', 'fa-solid fa-cart-shopping',
 'Support product sourcing, supplier coordination, purchase tracking and procurement documentation for agricultural and grocery supplies.',
 'Procurement, Vendor Management, Sourcing, Purchase Tracking', '6 Months', 'Based on the performance', 'ASAP',
 'Learn the procurement process from sourcing to purchase, working with agricultural suppliers and vendors.',
 '["Assist with vendor outreach","Track purchase orders","Maintain procurement records","Support sourcing activities"]',
 '["Analytical skills","Basic Excel knowledge","Good negotiation skills","Attention to detail"]',
 '["🌱 Farm Fresh Produce","📚 Learning Budget","🌴 Team Retreats","💰 Growth Bonuses","👨‍🏫 Mentorship"]',
 NULL, TRUE, 11),

(md5('vpnexues-career-job:digital-marketing')::uuid, 'digital-marketing', 'Digital Marketing Intern', 'Internship', 'Remote', 'fa-solid fa-chart-line',
 'Assist with digital campaigns, social media activities, content promotion, analytics and online brand visibility.',
 'Digital Marketing, Social Media, Content Marketing, Analytics', '6 Months', 'Based on the performance', 'ASAP',
 'Support our digital marketing efforts across social media, content, and online campaigns.',
 '["Manage social media posts","Assist with campaigns","Track analytics","Create content"]',
 '["Social media familiarity","Creative mindset","Basic analytics knowledge","Good writing skills"]',
 '["🌱 Farm Fresh Produce","📚 Learning Budget","🏠 Remote-First","🌴 Team Retreats","👨‍🏫 Mentorship"]',
 NULL, TRUE, 12),

(md5('vpnexues-career-job:android-dev')::uuid, 'android-dev', 'Android Developer', 'Internship', 'Remote', 'fa-brands fa-android',
 'Develop and maintain Android applications, integrate APIs, improve application performance and collaborate with the development team.',
 'Kotlin, Android SDK, REST APIs, Firebase', '6 Months', 'Based on the performance', 'ASAP',
 'Build mobile applications that connect farmers and customers across our organic supply chain.',
 '["Develop Android features","Integrate REST APIs","Fix bugs","Optimize performance"]',
 '["Android development experience","Kotlin/Java proficiency","API integration","Problem-solving skills"]',
 '["🌱 Farm Fresh Produce","📚 Learning Budget","🏠 Remote-First","🌴 Team Retreats","💰 Growth Bonuses","👨‍🏫 Mentorship"]',
 NULL, TRUE, 13),

(md5('vpnexues-career-job:b2b-sales')::uuid, 'b2b-sales', 'B2B / Retail Sales Executive Intern', 'Internship', 'On-Site & Field', 'fa-solid fa-handshake',
 'Identify and connect with potential B2B and retail customers, generate and follow up on sales leads, and achieve assigned sales targets for VPNexues.',
 'B2B Sales, Lead Generation, Negotiation, Communication', '6 Months', '₹10,000 - ₹15,000', 'ASAP',
 'Drive B2B and retail sales growth by connecting with customers, nurturing leads, and hitting targets for our organic product range.',
 '["Identify B2B and retail prospects","Generate and follow up leads","Promote organic products","Track sales targets","Maintain the lead database"]',
 '["Good communication skills","Target-driven mindset","Basic computer skills","Willingness to work remotely and in the field"]',
 '["🌱 Farm Fresh Produce","📚 Learning Budget","🌴 Team Retreats","💰 Growth Bonuses","👨‍🏫 Mentorship","🌍 Global Impact"]',
 NULL, TRUE, 14)
ON CONFLICT DO NOTHING;
