-- Delivery gate: the address must record WHICH COUNTRY it is in.
--
-- The checkout gate used to call isServiceable(headerCountrySelector, city).
-- The header country is a UI preference (backed by localStorage + GeoIP), not a
-- fact about the address, and user_addresses had no country column at all. So a
-- customer whose header selector sat on Singapore was validated against SG
-- coverage — which is a wildcard row ('*' = every city) — and ANY Indian city
-- passed, including non-deliverable ones. That is how a Tirupattur order
-- reached "Order Received" instead of the Coming Soon page.
--
-- This column lets checkout validate the city against the country the address
-- is ACTUALLY in. Existing rows are backfilled from `state` where we can infer
-- it; rows we cannot identify stay NULL and keep today's behaviour (gate on the
-- selected country), so no existing address can become less correct than before.
ALTER TABLE user_addresses
    ADD COLUMN IF NOT EXISTS country VARCHAR(2);

-- Backfill: Indian states + union territories -> 'IN'. Matched on the trimmed,
-- lowercased state so 'Tamil Nadu', 'tamil nadu ' and 'TAMIL NADU' agree.
UPDATE user_addresses
   SET country = 'IN'
 WHERE (country IS NULL OR TRIM(country) = '')
   AND state IS NOT NULL
   AND LOWER(TRIM(state)) IN (
        'andhra pradesh', 'arunachal pradesh', 'assam', 'bihar', 'chhattisgarh',
        'goa', 'gujarat', 'haryana', 'himachal pradesh', 'jharkhand', 'karnataka',
        'kerala', 'madhya pradesh', 'maharashtra', 'manipur', 'meghalaya', 'mizoram',
        'nagaland', 'odisha', 'orissa', 'punjab', 'rajasthan', 'sikkim', 'tamil nadu',
        'telangana', 'tripura', 'uttar pradesh', 'uttarakhand', 'west bengal',
        'andaman and nicobar islands', 'chandigarh', 'dadra and nagar haveli and daman and diu',
        'delhi', 'jammu and kashmir', 'ladakh', 'lakshadweep', 'puducherry', 'pondicherry'
   );

-- Backfill: Singapore regions -> 'SG'.
UPDATE user_addresses
   SET country = 'SG'
 WHERE (country IS NULL OR TRIM(country) = '')
   AND state IS NOT NULL
   AND LOWER(TRIM(state)) IN ('central region', 'east region', 'north region',
                              'north-east region', 'west region', 'singapore');
