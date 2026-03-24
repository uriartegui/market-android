import { Injectable, NotFoundException, ForbiddenException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { CreateProductDto } from './dto/create-product.dto';
import { UpdateProductDto } from './dto/update-product.dto';
import { AdjustStockDto, StockAdjustType } from './dto/adjust-stock.dto';

@Injectable()
export class ProductsService {
  constructor(private prisma: PrismaService) {}

  async create(dto: CreateProductDto, condominioId: string) {
    return this.prisma.product.create({
      data: {
        ...dto,
        category: dto.category ?? 'GERAL',
        condominioId,
      },
    });
  }

  async findAll(condominioId: string, category?: string, search?: string, lowStock?: boolean) {
    return this.prisma.product.findMany({
      where: {
        condominioId,
        active: true,
        ...(category && { category }),
        ...(search && {
          OR: [
            { name: { contains: search, mode: 'insensitive' } },
            { description: { contains: search, mode: 'insensitive' } },
            { barcode: { contains: search, mode: 'insensitive' } },
          ],
        }),
        ...(lowStock && { quantity: { lte: 5 } }),
      },
      orderBy: { name: 'asc' },
    });
  }

  async findByQrCode(qrCode: string, condominioId: string) {
    const product = await this.prisma.product.findFirst({
      where: { qrCode, condominioId, active: true },
    });
    if (!product) throw new NotFoundException('Produto não encontrado');
    return product;
  }

  async findByBarcode(barcode: string, condominioId: string) {
    const product = await this.prisma.product.findFirst({
      where: { barcode, condominioId, active: true },
    });
    if (!product) throw new NotFoundException('Produto não encontrado');
    return product;
  }

  async findOne(id: string, condominioId: string) {
    const product = await this.prisma.product.findFirst({
      where: { id, condominioId },
    });
    if (!product) throw new NotFoundException('Produto não encontrado');
    return product;
  }

  async update(id: string, dto: UpdateProductDto, condominioId: string) {
    await this.findOne(id, condominioId);
    return this.prisma.product.update({
      where: { id },
      data: dto,
    });
  }

  async adjustStock(id: string, dto: AdjustStockDto, condominioId: string) {
    const product = await this.findOne(id, condominioId);

    let newQuantity: number;

    switch (dto.type) {
      case StockAdjustType.ADD:
        newQuantity = product.quantity + dto.quantity;
        break;
      case StockAdjustType.REMOVE:
        newQuantity = Math.max(0, product.quantity - dto.quantity);
        break;
      case StockAdjustType.SET:
        newQuantity = dto.quantity;
        break;
    }

    return this.prisma.product.update({
      where: { id },
      data: { quantity: newQuantity },
    });
  }

  async remove(id: string, condominioId: string) {
    await this.findOne(id, condominioId);
    return this.prisma.product.update({
      where: { id },
      data: { active: false },
    });
  }
}
