import { Module } from '@nestjs/common';
import { PrismaModule } from '../prisma/prisma.module';
import { NotificationService } from './notification.service';
import { TokenExpiryScheduler } from './token-expiry.scheduler';

@Module({
  imports: [PrismaModule],
  providers: [NotificationService, TokenExpiryScheduler],
  exports: [NotificationService, TokenExpiryScheduler],
})
export class NotificationsModule {}
