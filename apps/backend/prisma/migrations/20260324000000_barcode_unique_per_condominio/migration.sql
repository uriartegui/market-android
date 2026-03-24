-- Remover constraints globais de barcode e qrCode
DROP INDEX IF EXISTS "Product_barcode_key";
DROP INDEX IF EXISTS "Product_qrCode_key";

-- Criar constraints compostas (único por condomínio)
CREATE UNIQUE INDEX IF NOT EXISTS "Product_barcode_condominioId_key" ON "Product"("barcode", "condominioId");
CREATE UNIQUE INDEX IF NOT EXISTS "Product_qrCode_condominioId_key" ON "Product"("qrCode", "condominioId");
