-- AlterTable
ALTER TABLE "Product" ADD COLUMN "qrCode" TEXT;

-- CreateIndex
CREATE UNIQUE INDEX "Product_qrCode_key" ON "Product"("qrCode");
