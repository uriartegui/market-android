import {
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { CreateProductDto } from './dto/create-product.dto';
import { UpdateProductDto } from './dto/update-product.dto';
import * as QRCode from 'qrcode';

@Injectable()
export class ProductsService {
  constructor(private prisma: PrismaService) {}

  async create(dto: CreateProductDto, condominioId: string) {
    const product = await this.prisma.product.create({
      data: {
        ...dto,
        condominioId,
      },
    });

    const qrCode = await QRCode.toDataURL(`product:${product.id}`);

    return this.prisma.product.update({
      where: { id: product.id },
      data: { qrCode },
    });
  }

  async findAll(condominioId: string, category?: string) {
    return this.prisma.product.findMany({
      where: {
        condominioId,
        active: true,
        ...(category && { category }),
      },
      orderBy: { createdAt: 'desc' },
    });
  }

  async findOne(id: string, condominioId: string) {
    const product = await this.prisma.product.findFirst({
      where: { id, condominioId },
    });

    if (!product) throw new NotFoundException('Produto não encontrado');
    return product;
  }

  async findByQrCode(qrCode: string) {
    const decoded = decodeURIComponent(qrCode);

    const product = await this.prisma.product.findFirst({
      where: {
        OR: [
          { qrCode: decoded },
          { qrCode: qrCode },
        ],
        active: true,
      },
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

  async remove(id: string, condominioId: string) {
    await this.findOne(id, condominioId);

    await this.prisma.product.update({
      where: { id },
      data: { active: false },
    });

    return { message: 'Produto removido com sucesso' };
  }

  async findByBarcode(barcode: string) {
    const product = await this.prisma.product.findFirst({
      where: { barcode, active: true },
    });

    if (!product) throw new NotFoundException('Produto não encontrado');
    return product;
  }
}

