import {
  Injectable,
  NotFoundException,
  BadRequestException,
} from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import MercadoPagoConfig, { Payment } from 'mercadopago';
import { PaymentStatus } from '@prisma/client';

@Injectable()
export class PaymentsService {
  private client: MercadoPagoConfig;

  constructor(private prisma: PrismaService) {
    this.client = new MercadoPagoConfig({
      accessToken: process.env.MERCADOPAGO_ACCESS_TOKEN!,
    });
  }

  async createPixPayment(orderId: string, userId: string) {
    const order = await this.prisma.order.findFirst({
      where: { id: orderId, userId },
      include: { user: true },
    });

    if (!order) throw new NotFoundException('Pedido não encontrado');

    if (order.status !== 'PENDENTE') {
      throw new BadRequestException('Pedido já foi pago ou cancelado');
    }

    const existingPayment = await this.prisma.payment.findUnique({
      where: { orderId },
    });

    if (existingPayment && existingPayment.status === 'PENDING') {
      return existingPayment;
    }

    const paymentClient = new Payment(this.client);

    const mpPayment = await paymentClient.create({
      body: {
        transaction_amount: Number(order.total),
        description: `Pedido #${order.id.slice(0, 8)}`,
        payment_method_id: 'pix',
        payer: {
          email: order.user?.email ?? 'cliente@mercadinho.com',
        },        
      },
    });

    const expiresAt = new Date();
    expiresAt.setMinutes(expiresAt.getMinutes() + 30);

    const payment = await this.prisma.payment.create({
      data: {
        orderId,
        amount: order.total,
        mercadoPagoId: String(mpPayment.id),
        pixQrCode:
          mpPayment.point_of_interaction?.transaction_data?.qr_code ?? null,
        pixQrCodeBase64:
          mpPayment.point_of_interaction?.transaction_data?.qr_code_base64 ??
          null,
        expiresAt,
      },
    });

    return payment;
  }

  async getPaymentStatus(orderId: string) {
    const payment = await this.prisma.payment.findUnique({
      where: { orderId },
    });

    if (!payment) throw new NotFoundException('Pagamento não encontrado');

    if (payment.mercadoPagoId) {
      const paymentClient = new Payment(this.client);
      const mpPayment = await paymentClient.get({
        id: payment.mercadoPagoId,
      });

      let status: PaymentStatus = payment.status;

      if (mpPayment.status === 'approved') status = 'APPROVED';
      else if (mpPayment.status === 'rejected') status = 'REJECTED';

      if (status !== payment.status) {
        await this.prisma.payment.update({
          where: { orderId },
          data: { status },
        });

        if (status === 'APPROVED') {
          await this.prisma.order.update({
            where: { id: orderId },
            data: { status: 'PAGO' },
          });
        }
      }

      return { ...payment, status };
    }

    return payment;
  }

  async handleWebhook(data: { type?: string; data?: { id?: string } }) {
    if (data.type !== 'payment' || !data.data?.id) return;

    const paymentClient = new Payment(this.client);
    const mpPayment = await paymentClient.get({
      id: String(data.data.id),
    });

    const payment = await this.prisma.payment.findFirst({
      where: { mercadoPagoId: String(mpPayment.id) },
    });

    if (!payment) return;

    let status: PaymentStatus = payment.status;
    if (mpPayment.status === 'approved') status = 'APPROVED';
    else if (mpPayment.status === 'rejected') status = 'REJECTED';

    await this.prisma.payment.update({
      where: { id: payment.id },
      data: { status },
    });

    if (status === 'APPROVED') {
      await this.prisma.order.update({
        where: { id: payment.orderId },
        data: { status: 'PAGO' },
      });
    }
  }
}
