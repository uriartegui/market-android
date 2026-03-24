import { Injectable, Logger } from '@nestjs/common';
import { Cron, CronExpression } from '@nestjs/schedule';
import { PrismaService } from '../prisma/prisma.service';
import { NotificationService } from './notification.service';

@Injectable()
export class TokenExpiryScheduler {
  private readonly logger = new Logger(TokenExpiryScheduler.name);

  constructor(
    private prisma: PrismaService,
    private notificationService: NotificationService,
  ) {}

  // Roda todo dia às 09:00 (horário de Brasília = UTC-3, então 12:00 UTC)
  @Cron('0 12 * * *')
  async checkExpiringTokens() {
    this.logger.log('Verificando tokens próximos de expirar...');

    const daysToCheck = [3, 2, 1];

    for (const days of daysToCheck) {
      const start = new Date();
      start.setDate(start.getDate() + days);
      start.setHours(0, 0, 0, 0);

      const end = new Date(start);
      end.setHours(23, 59, 59, 999);

      const tokens = await this.prisma.refreshToken.findMany({
        where: {
          expiresAt: {
            gte: start,
            lte: end,
          },
        },
      });

      if (tokens.length > 0) {
        this.logger.log(`${tokens.length} token(s) expirando em ${days} dia(s)`);
        await this.notificationService.notifyTokenExpiringSoon(days, tokens.length);
      }
    }
  }
}
