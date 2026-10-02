-- Public /team and /news content, managed from the admin dashboard.
-- Seeded from the static site data (teamData.ts TEAM, BlogDetail.tsx ARTICLES)
-- so the pages render identically after switching to API-backed content.
-- ids are deterministic md5 UUIDs: a re-run on another machine derives the same
-- rows without needing ON CONFLICT guards.

CREATE TABLE team_members (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug           VARCHAR(255) NOT NULL UNIQUE,
    name           VARCHAR(255) NOT NULL,
    role           VARCHAR(255),
    photo_url      TEXT,
    description    TEXT,
    is_director    BOOLEAN NOT NULL DEFAULT FALSE,
    display_order  INTEGER NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE news_articles (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legacy_key     VARCHAR(10) UNIQUE,
    title          VARCHAR(255) NOT NULL,
    author         VARCHAR(255) NOT NULL DEFAULT 'Admin',
    image_url      TEXT NOT NULL,
    excerpt        TEXT,
    content        TEXT NOT NULL,
    tags           VARCHAR(255),
    published_date DATE NOT NULL DEFAULT CURRENT_DATE,
    read_time      VARCHAR(20) NOT NULL DEFAULT '3 min',
    comments       INTEGER NOT NULL DEFAULT 0,
    display_order  INTEGER NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_team_members_display_order ON team_members (display_order);
CREATE INDEX idx_news_articles_published_date ON news_articles (published_date DESC);

-- ---------------------------------------------------------------------------
-- Seed: team members (mirrors TEAM in vpnexues-ui/src/pages/marketing/teamData.ts)
-- ---------------------------------------------------------------------------
INSERT INTO team_members (id, slug, name, role, photo_url, description, is_director, display_order)
VALUES
(md5('vpnexues-team:nageswaran-duraiswamy')::uuid, 'nageswaran-duraiswamy', 'Nageswaran Duraiswamy', NULL, '/images/team/nageswaran.jpg', $bio$Nageswaran is one of the directors at VPNexues, helping set the company's overall direction.$bio$, TRUE, 0),
(md5('vpnexues-team:pradeep-kumar')::uuid, 'pradeep-kumar', 'Pradeep Kumar', NULL, '/images/team/pradeep.jpeg', $bio$Pradeep is one of the directors at VPNexues, helping set the company's overall direction.$bio$, TRUE, 1),
(md5('vpnexues-team:libin-k')::uuid, 'libin-k', 'Libin K', 'Team Lead', '/images/team/libin.jpg', $bio$Libin is part of the VPNexues team, focused on Leadership.$bio$, FALSE, 2),
(md5('vpnexues-team:hemaanth-kumar-t-n')::uuid, 'hemaanth-kumar-t-n', 'Hemaanth Kumar T N', 'Web Developer', '/images/team/Hemaanth.jpeg', $bio$Hemaanth is part of the VPNexues team, focused on Development.$bio$, FALSE, 3),
(md5('vpnexues-team:sathish-r')::uuid, 'sathish-r', 'Sathish R', 'Web Developer', '/images/team/Satheesh.jpeg', $bio$Sathish is part of the VPNexues team, focused on Development.$bio$, FALSE, 4),
(md5('vpnexues-team:monika-v')::uuid, 'monika-v', 'Monika V', 'UI/UX Designer', '/images/team/Monika.jpeg', $bio$Monika is part of the VPNexues team, focused on Design.$bio$, FALSE, 5),
(md5('vpnexues-team:sunil-kumar-r')::uuid, 'sunil-kumar-r', 'Sunil Kumar R', 'Web Developer', '/images/team/SunilKumar.jpeg', $bio$Sunil is part of the VPNexues team, focused on Development.$bio$, FALSE, 6),
(md5('vpnexues-team:jegatheeswaran')::uuid, 'jegatheeswaran', 'Jegatheeswaran', 'Web Developer', '/images/team/Jagadeesh.jpeg', $bio$Jegatheeswaran is part of the VPNexues team, focused on Development.$bio$, FALSE, 7),
(md5('vpnexues-team:keerthi-vardhan')::uuid, 'keerthi-vardhan', 'Keerthi Vardhan', 'UI/UX Designer', '/images/team/Keerthi.jpeg', $bio$Keerthi is part of the VPNexues team, focused on Design.$bio$, FALSE, 8),
(md5('vpnexues-team:shereen')::uuid, 'shereen', 'Shereen', 'HR Executive', '/images/team/Shreen.jpeg', $bio$Shereen is part of the VPNexues team, focused on HR.$bio$, FALSE, 9),
(md5('vpnexues-team:kiruba')::uuid, 'kiruba', 'Kiruba', 'Junior HR Executive', '/images/team/Kiruba.webp', $bio$Kiruba is part of the VPNexues team, focused on HR.$bio$, FALSE, 10),
(md5('vpnexues-team:anees')::uuid, 'anees', 'Anees', 'Android/iOS Developer', '/images/team/Annes.jpeg', $bio$Anees is part of the VPNexues team, focused on Development.$bio$, FALSE, 11),
(md5('vpnexues-team:arun')::uuid, 'arun', 'Arun', 'Module Lead', '/images/team/Arun.jpeg', $bio$Arun is Module Lead at VPNexues, owning key modules while mentoring the team.$bio$, FALSE, 12),
(md5('vpnexues-team:sachin')::uuid, 'sachin', 'Sachin', 'Android/iOS Developer', '/images/team/Sachein.jpeg', $bio$Sachin is part of the VPNexues team, focused on Development.$bio$, FALSE, 13),
(md5('vpnexues-team:siddhika')::uuid, 'siddhika', 'Siddhika', 'Accountant', '/images/team/siddhika.jpeg', $bio$Siddhika is part of the VPNexues team, focused on Finance.$bio$, FALSE, 14),
(md5('vpnexues-team:gurudheep')::uuid, 'gurudheep', 'Gurudheep', 'Web Developer', '/images/team/gurudheep.jpeg', $bio$Gurudheep is part of the VPNexues team, focused on Development.$bio$, FALSE, 15),
(md5('vpnexues-team:pavithra')::uuid, 'pavithra', 'Pavithra', 'Module Lead', '/images/team/Pavithra.jpeg', $bio$Pavithra is Module Lead at VPNexues, owning key modules while mentoring the team.$bio$, FALSE, 16),
(md5('vpnexues-team:jacith')::uuid, 'jacith', 'Jacinth', 'Android Developer', '/images/team/jacinth.jpeg', $bio$Jacinth is part of the VPNexues team, focused on Android Development.$bio$, FALSE, 17),
(md5('vpnexues-team:kokila-vani')::uuid, 'kokila-vani', 'Kokila Vani', 'SEO Content Writer', '/images/team/kokila%20vani.jpeg', $bio$Kokila Vani is part of the VPNexues team, focused on Content & SEO.$bio$, FALSE, 18),
(md5('vpnexues-team:rohan')::uuid, 'rohan', 'Rohan', 'SEO Content Writer', '/images/team/rohan.jpeg', $bio$Rohan is part of the VPNexues team, focused on Content & SEO.$bio$, FALSE, 19),
(md5('vpnexues-team:mahmooda-fareeha')::uuid, 'mahmooda-fareeha', 'Mahmooda Fareeha', 'Junior HR Executive', '/images/team/Mahmooda%20Fareeha.png', $bio$Mahmooda Fareeha is part of the VPNexues team, focused on HR.$bio$, FALSE, 20),
(md5('vpnexues-team:pavithra-u-v')::uuid, 'pavithra-u-v', 'Pavithra U V', 'Business Development', '/images/team/pavithra%20uv.jpeg', $bio$Pavithra U V is part of the VPNexues team, focused on Business Development.$bio$, FALSE, 21),
(md5('vpnexues-team:rasika')::uuid, 'rasika', 'Rasika', 'SEO Writer', '/images/team/rasika.jpeg', $bio$Rasika is part of the VPNexues team, focused on Content & SEO.$bio$, FALSE, 22);

-- ---------------------------------------------------------------------------
-- Seed: news articles (mirrors ARTICLES in vpnexues-ui/src/pages/marketing/BlogDetail.tsx)
-- legacy_key keeps the link to the rich static layout (subheadings/checklist/quote/comments)
-- so unchanged articles render exactly as before; content paragraphs are separated by
-- blank lines, which the public pages split on.
-- ---------------------------------------------------------------------------
INSERT INTO news_articles (id, legacy_key, title, author, image_url, excerpt, content, tags, published_date, read_time, comments, display_order)
VALUES
(md5('vpnexues-news:1')::uuid, '1', 'Sustainable Farming: Feeding the Future Responsibly', 'Pradeep K.',
 'https://res.cloudinary.com/eqjshlur/image/upload/v1790001873/vpnexues/old/site-assets/news/news-sustainable.jpg',
 'Discover how VPNexues integrates organic practices with modern supply chains to deliver farm-fresh produce across the globe.',
$$Discover how VPNexues integrates organic practices with modern supply chains to deliver farm-fresh produce across the globe. From the fertile red soil of Tamil Nadu to dining tables in Singapore, the UAE and Europe, every single harvest follows one clear promise — purity without compromise, and uncompromising quality control at every stage of the journey.

Conventional farming relies heavily on chemical inputs that steadily degrade soil health, groundwater reserves and surrounding biodiversity over time. Sustainable farming completely reverses that trend by working together with nature instead of fighting against it. When the soil is healthy, the crops it grows are healthier too, and when the crops are healthier, the people who eat them feel the difference in every single meal they enjoy.

From farm to table, without compromise — every step of the supply chain is designed to protect freshness, preserve nutrition, and support local farming communities. By shortening the distance between harvest and home, we significantly reduce waste while locking in peak flavour and nutritional value that mass-produced alternatives simply cannot match.$$,
 'Organic,Export,Farm,Fresh', DATE '2026-06-12', '3 min', 4, 0),

(md5('vpnexues-news:2')::uuid, '2', 'From Farm to Table: The Journey of Organic Spices', 'Nageswaran D.',
 'https://res.cloudinary.com/eqjshlur/image/upload/v1790001870/vpnexues/old/site-assets/news/news-spices.jpg',
 'Explore the traceability and quality control that makes VPNexues spices stand out in international markets.',
$$Explore the traceability and quality control that makes VPNexues spices stand out in international markets. Every spice that leaves our facility carries the story of the farmer who grew it, the soil that nurtured it, and the care that preserved its natural goodness from field to fork.

Quality control is not a single checkpoint — it is a continuous process that spans harvesting, cleaning, processing, testing, packaging and shipping. At every stage, our team ensures that international food safety standards are not just met but exceeded.

The global demand for authentic, organic Indian spices continues to rise as consumers become more conscious about what goes into their food. VPNexues has positioned itself at the intersection of traditional farming wisdom and modern supply chain efficiency.$$,
 'Spices,Organic,Export', DATE '2026-06-01', '4 min', 7, 1),

(md5('vpnexues-news:3')::uuid, '3', 'Export Trends: Indian Organic Produce in 2026', 'Admin',
 'https://res.cloudinary.com/eqjshlur/image/upload/v1790001868/vpnexues/old/site-assets/news/news-export.jpg',
 'Insights into the growing demand for Indian organic products and how VPNexues is leading the charge.',
$$Insights into the growing demand for Indian organic products and how VPNexues is leading the charge. The global organic food market continues to expand at an unprecedented rate, with Indian exports playing a pivotal role in meeting this rising demand.

India's organic export landscape has transformed dramatically over the past few years. What was once a niche market dominated by a handful of players has now become a thriving ecosystem of certified producers, each contributing to the nation's organic legacy.

As more countries tighten regulations around food safety and sustainability, Indian organic producers who maintain rigorous certification standards are finding unprecedented opportunities in international markets.$$,
 'Export,Organic,Farm', DATE '2026-05-15', '3 min', 2, 2),

(md5('vpnexues-news:4')::uuid, '4', 'Freshness You Can Taste: Our Cold-Chain Promise', 'Pradeep K.',
 'https://res.cloudinary.com/eqjshlur/image/upload/v1790156219/vpnexues/banners/page-banner-pricing.jpg',
 'Every product that leaves our facility is packed with peak freshness preserved through our advanced cold-chain logistics network.',
$$Every product that leaves our facility is packed with peak freshness preserved through our advanced cold-chain logistics network. From the moment produce is harvested, temperature-controlled systems ensure that nutrition, flavour and texture remain intact all the way to your table.

Our cold-chain infrastructure spans across Tamil Nadu, connecting remote organic farms to our centrally located processing hubs. Rapid collection and same-day processing means the gap between harvest and packaging is measured in hours, not days.

This commitment to freshness is what sets VPNexues apart in a market where shelf-life often comes at the cost of taste and nutrition. We believe you should never have to choose between convenience and quality.$$,
 'Fresh,Organic,Farm', DATE '2026-05-05', '3 min', 3, 3),

(md5('vpnexues-news:5')::uuid, '5', '100% Plant-Based: Organic Vegan Produce from Tamil Nadu', 'Hemaanth R.',
 'https://res.cloudinary.com/eqjshlur/image/upload/v1790168677/vpnexues/news/organic-vegan.jpg',
 'From naturally grown vegetables to plant-based staples, VPNexues delivers certified organic vegan products to health-conscious consumers worldwide.',
$$From naturally grown vegetables to plant-based staples, VPNexues delivers certified organic vegan products to health-conscious consumers worldwide. The demand for plant-based food has surged globally, and Tamil Nadu is uniquely positioned to lead this movement.

Our vegan product range includes organic millets, cold-pressed oils, naturally dried fruits, heirloom rice varieties and fresh vegetables — all grown without synthetic chemicals, pesticides or animal-derived inputs.

Every product carries India Organic and USDA NOP certifications, giving international buyers the confidence that what they receive is genuinely plant-based and produced to the highest ethical standards.$$,
 'Organic,Farm,Fresh', DATE '2026-04-20', '4 min', 5, 4),

(md5('vpnexues-news:6')::uuid, '6', 'Pride of Tamil Nadu: Supporting Local Organic Farmers', 'Nageswaran D.',
 'https://res.cloudinary.com/eqjshlur/image/upload/v1790167072/vpnexues/news/local-organic.jpg',
 'VPNexues partners with over 200 organic farms across Tamil Nadu, ensuring fair prices and sustainable practices for every farmer in our network.',
$$VPNexues partners with over 200 organic farms across Tamil Nadu, ensuring fair prices and sustainable practices for every farmer in our network. Our direct-sourcing model eliminates middlemen and puts more money in the hands of the people who grow our food.

Tamil Nadu's rich agricultural heritage spans centuries. The red soil of Salem, the river basins of Thanjavur and the highlands of Nilgiris each produce unique crops with distinct flavours that cannot be replicated elsewhere.

By investing in farmer training, soil health programs and organic certification support, we are building an ecosystem where sustainable farming is not just possible — it is profitable.$$,
 'Organic,Farm,Fresh', DATE '2026-04-10', '3 min', 6, 5),

(md5('vpnexues-news:7')::uuid, '7', 'Premium Grade: Handpicked Organic Spices for Global Kitchens', 'Libin K.',
 'https://res.cloudinary.com/eqjshlur/image/upload/v1790001863/vpnexues/old/site-assets/news/news-banner-main.jpg',
 'Our premium spice collection is carefully handpicked, tested for purity and packaged to preserve the authentic flavour that international chefs demand.',
$$Our premium spice collection is carefully handpicked, tested for purity and packaged to preserve the authentic flavour that international chefs demand. Each spice undergoes rigorous quality testing before it earns the VPNexues Premium badge.

From the fiery red chillies of Guntur to the aromatic cardamom of the Western Ghats, our premium range represents the finest that Indian agriculture has to offer. Every batch is lab-tested for chemical residues, moisture content and essential oil concentration.

International chefs and specialty food stores choose VPNexues premium spices because they know consistency matters. Every shipment delivers the same exceptional quality, batch after batch, year after year.$$,
 'Organic,Spices,Export', DATE '2026-03-28', '5 min', 4, 6);
