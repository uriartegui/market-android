import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { CreateOrderDto } from './dto/create-order.dto';
import { OrderStatus } from '@prisma/client';

@Injectable()
export class OrdersService {
  constructor(private prisma: PrismaService) {}

  async create(dto: CreateOrderDto, userId: string, condominioId: string) {
    const items = await Promise.all(
      dto.items.map(async (item) => {
        const product = await this.prisma.product.findUnique({
          where: { id: item.productId },
        });
        if (!product) throw new NotFoundException(`Produto ${item.productId} não encontrado`);
        return { ...item, price: product.price };
      }),
    );

    const total = items.reduce((sum, item) => sum + Number(item.price) * item.quantity, 0);

    const order = await this.prisma.order.create({
      data: {
        userId,
        condominioId,
        total,
        items: {
          create: items.map((item) => ({
            productId: item.productId,
            quantity: item.quantity,
            price: item.price,
          })),
        },
      },
      include: { items: true },
    });

    // Baixar estoque
    for (const item of items) {
      await this.prisma.product.update({
        where: { id: item.productId },
        data: { quantity: { decrement: item.quantity } },
      });
    }

    return order;
  }

  async createKiosk(dto: CreateOrderDto, condominioId: string) {
    const items = await Promise.all(
      dto.items.map(async (item) => {
        const product = await this.prisma.product.findUnique({
          where: { id: item.productId },
        });
        if (!product) throw new NotFoundException(`Produto ${item.productId} não encontrado`);
        return { ...item, price: product.price };
      }),
    );

    const total = items.reduce((sum, item) => sum + Number(item.price) * item.quantity, 0);

    const order = await this.prisma.order.create({
      data: {
        condominioId,
        total,
        items: {
          create: items.map((item) => ({
            productId: item.productId,
            quantity: item.quantity,
            price: item.price,
          })),
        },
      },
      include: { items: true },
    });

    // Baixar estoque
    for (const item of items) {
      await this.prisma.product.update({
        where: { id: item.productId },
        data: { quantity: { decrement: item.quantity } },
      });
    }

    return order;
  }

  async findAll(
    condominioId: string,
    status?: OrderStatus,
    startDate?: string,
    endDate?: string,
    page = 1,
    limit = 20,
  ) {
    const where: any = { condominioId };

    if (status) where.status = status;
    if (startDate || endDate) {
      where.createdAt = {};
      if (startDate) where.createdAt.gte = new Date(startDate);
      if (endDate) where.createdAt.lte = new Date(endDate);
    }

    const [orders, total] = await Promise.all([
      this.prisma.order.findMany({
        where,
        include: {
          items: {
            include: { product: { select: { name: true, imageUrl: true } } },
          },
          payment: true,
        },
        orderBy: { createdAt: 'desc' },
        skip: (page - 1) * limit,
        take: limit,
      }),
      this.prisma.order.count({ where }),
    ]);

    return { orders, total, page, limit, totalPages: Math.ceil(total / limit) };
  }

  async findOne(id: string, condominioId: string) {
    const order = await this.prisma.order.findFirst({
      where: { id, condominioId },
      include: {
        items: {
          include: { product: { select: { name: true, imageUrl: true, price: true } } },
        },
        payment: true,
      },
    });
    if (!order) throw new NotFoundException('Pedido não encontrado');
    return order;
  }

  async updateStatus(id: string, status: OrderStatus, condominioId: string) {
    await this.findOne(id, condominioId);
    return this.prisma.order.update({
      where: { id },
      data: { status },
    });
  }

  async getStats(condominioId: string) {
    const now = new Date();
    const startOfDay = new Date(now.setHours(0, 0, 0, 0));
    const startOfWeek = new Date(now);
    startOfWeek.setDate(now.getDate() - 7);
    const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);

    const [today, week, month, allTime, pending] = await Promise.all([
      this.prisma.order.aggregate({
        where: { condominioId, status: 'PAGO', createdAt: { gte: startOfDay } },
        _sum: { total: true },
        _count: true,
      }),
      this.prisma.order.aggregate({
        where: { condominioId, status: 'PAGO', createdAt: { gte: startOfWeek } },
        _sum: { total: true },
        _count: true,
      }),
      this.prisma.order.aggregate({
        where: { condominioId, status: 'PAGO', createdAt: { gte: startOfMonth } },
        _sum: { total: true },
        _count: true,
      }),
      this.prisma.order.aggregate({
        where: { condominioId, status: 'PAGO' },
        _sum: { total: true },
        _count: true,
      }),
      this.prisma.order.count({
        where: { condominioId, status: 'PENDENTE' },
      }),
    ]);

    return {
      today: { total: today._sum.total ?? 0, count: today._count },
      week: { total: week._sum.total ?? 0, count: week._count },
      month: { total: month._sum.total ?? 0, count: month._count },
      allTime: { total: allTime._sum.total ?? 0, count: allTime._count },
      pendingOrders: pending,
    };
  }
}
