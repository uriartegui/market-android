import { Injectable, ConflictException, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { CreateCondominioDto } from './dto/create-condominio.dto';

@Injectable()
export class CondominioService {
  constructor(private prisma: PrismaService) {}

  async create(dto: CreateCondominioDto) {
    const exists = await this.prisma.condominio.findUnique({
      where: { code: dto.code },
    });

    if (exists) throw new ConflictException('Código de condomínio já existe');

    return this.prisma.condominio.create({
      data: dto,
    });
  }

  async findAll() {
    return this.prisma.condominio.findMany({
      select: {
        id: true,
        name: true,
        address: true,
        code: true,
        createdAt: true,
        _count: { select: { users: true, products: true } },
      },
    });
  }

  async findOne(id: string) {
    const cond = await this.prisma.condominio.findUnique({
      where: { id },
      select: {
        id: true,
        name: true,
        address: true,
        code: true,
        createdAt: true,
        _count: { select: { users: true, products: true } },
      },
    });

    if (!cond) throw new NotFoundException('Condomínio não encontrado');
    return cond;
  }

  async join(userId: string, code: string) {
    const cond = await this.prisma.condominio.findUnique({
      where: { code },
    });

    if (!cond) throw new NotFoundException('Código inválido');

    await this.prisma.user.update({
      where: { id: userId },
      data: { condominioId: cond.id },
    });

    return { message: 'Vinculado ao condomínio com sucesso', condominio: cond };
  }
}
