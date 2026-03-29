import { Injectable, Logger } from '@nestjs/common';
import { Cron } from '@nestjs/schedule';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class PaymentExpiryScheduler {
  private readonly logger = new Logger(PaymentExpiryScheduler.name);

  constructor(private readonly prisma: PrismaService) {}

  // Roda a cada 5 minutos
  @Cron('*/5 * * * *')
  async expireStalePayments() {
    const now = new Date();

    const expired = await this.prisma.payment.findMany({
      where: {
        status: 'PENDING',
        expiresAt: { lt: now },
      },
      select: { id: true, orderId: true },
    });

    if (expired.length === 0) return;

    this.logger.log(`Expirando ${expired.length} pagamento(s) vencido(s)...`);

    const paymentIds = expired.map((p) => p.id);
    const orderIds   = expired.map((p) => p.orderId);

    await this.prisma.payment.updateMany({
      where: { id: { in: paymentIds } },
      data:  { status: 'EXPIRED' },
    });

    await this.prisma.order.updateMany({
      where: { id: { in: orderIds }, status: 'PENDENTE' },
      data:  { status: 'CANCELADO' },
    });

    this.logger.log(`Pedidos cancelados: ${orderIds.join(', ')}`);
  }
}
