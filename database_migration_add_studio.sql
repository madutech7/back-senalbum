-- Migration pour ajouter le plan STUDIO et les nouveaux champs de branding

-- 1. Supprimer l'ancienne contrainte de vérification
ALTER TABLE photographers DROP CONSTRAINT IF EXISTS photographers_subscription_plan_check;

-- 2. Ajouter la nouvelle contrainte avec STUDIO
ALTER TABLE photographers 
ADD CONSTRAINT photographers_subscription_plan_check 
CHECK (subscription_plan IN ('FREE', 'PRO', 'STUDIO'));

-- 3. Ajouter les colonnes de branding si elles n'existent pas
ALTER TABLE photographers 
ADD COLUMN IF NOT EXISTS brand_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS brand_logo_url TEXT,
ADD COLUMN IF NOT EXISTS brand_cover_url TEXT,
ADD COLUMN IF NOT EXISTS brand_primary_color VARCHAR(7),
ADD COLUMN IF NOT EXISTS custom_domain VARCHAR(255) UNIQUE;

-- 4. Maintenant vous pouvez mettre à jour le plan vers STUDIO
UPDATE photographers 
SET subscription_plan = 'STUDIO' 
WHERE id = '2005c612-2197-4939-b737-53fac276bdc7';

-- 5. Vérification
SELECT id, email, subscription_plan, brand_name, custom_domain 
FROM photographers 
WHERE id = '2005c612-2197-4939-b737-53fac276bdc7';
