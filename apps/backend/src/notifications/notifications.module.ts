import { Module } from '@nestjs/common';
import { NotificationService } from './notification.service';
import { TokenExpiryScheduler } from './token-expiry.scheduler';

@Module({
  providers: [NotificationService, TokenExpiryScheduler],
  exports: [NotificationService, TokenExpiryScheduler],
})
export class NotificationsModule {}
