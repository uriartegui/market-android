import { Injectable, NotFoundException, ConflictException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { UpdateUserDto } from './dto/update-user.dto';
import * as bcrypt from 'bcrypt';

@Injectable()
export class UsersService {
  constructor(private prisma: PrismaService) {}

  async findById(id: string) {
    const user = await this.prisma.user.findUnique({ where: { id } });
    if (!user) throw new NotFoundException('Usuário não encontrado');
    return user;
  }

  async update(id: string, dto: UpdateUserDto) {
    return this.prisma.user.update({
      where: { id },
      data: dto,
    });
  }

  async createKioskUser(condominioId: string, name: string) {
    const email = `kiosk-${condominioId}@market.internal`;

    const existing = await this.prisma.user.findUnique({ where: { email } });
    if (existing) throw new ConflictException('Usuário kiosk já existe para este estoque');

    const password = await bcrypt.hash(`kiosk-${condominioId}`, 10);

    return this.prisma.user.create({
      data: {
        name,
        email,
        password,
        role: 'KIOSK',
        condominioId,
      },
      select: {
        id: true,
        name: true,
        email: true,
        role: true,
        condominioId: true,
      },
    });
  }

  async getKioskUser(condominioId: string) {
    const email = `kiosk-${condominioId}@market.internal`;
    const user = await this.prisma.user.findUnique({
      where: { email },
      select: {
        id: true,
        name: true,
        email: true,
        role: true,
        condominioId: true,
      },
    });
    if (!user) throw new NotFoundException('Usuário kiosk não encontrado');
    return user;
  }
}
